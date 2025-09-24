package ru.hh.jclient.common.balancing;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import ru.hh.jclient.common.HttpClientFactoryBuilder;
import static ru.hh.jclient.common.JClientBase.HTTP_POST;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestEngine;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResponseWrapper;

public abstract class RequestBalancer implements RequestEngine {
  private static final Logger LOGGER = LoggerFactory.getLogger(RequestBalancer.class);
  static final int WARM_UP_DEFAULT_TIME_MILLIS = 100;

  private final RequestStrategy.RequestExecutor requestExecutor;
  private final RetryPolicy retryPolicy;
  private final boolean forceIdempotence;
  private int triesLeft;
  private int requestTimeLeftMs;
  private final double timeoutMultiplier;
  private final Level balancingRequestsLogLevel;

  protected final Request request;
  protected final int maxTries;
  protected final List<TraceFrame> trace;

  RequestBalancer(
      Request request,
      RequestStrategy.RequestExecutor requestExecutor,
      RetryPolicy retryPolicy,
      int requestTimeoutMs,
      int maxRequestTimeoutTries,
      int maxTries,
      @Nullable Double timeoutMultiplier,
      String balancingRequestsLogLevel,
      boolean forceIdempotence
  ) {
    this.retryPolicy = retryPolicy;
    this.timeoutMultiplier = Optional.ofNullable(timeoutMultiplier).orElse(HttpClientFactoryBuilder.DEFAULT_TIMEOUT_MULTIPLIER);
    this.balancingRequestsLogLevel = Level.valueOf(balancingRequestsLogLevel.toUpperCase());
    this.request = request;
    this.requestExecutor = requestExecutor;
    this.forceIdempotence = forceIdempotence;

    requestTimeoutMs = (int) ((request.getRequestTimeout() > 0 ? request.getRequestTimeout() : requestTimeoutMs) * this.timeoutMultiplier);

    requestTimeLeftMs = requestTimeoutMs * maxRequestTimeoutTries;
    this.maxTries = maxTries;
    this.trace = new ArrayList<>();

    triesLeft = this.maxTries;
  }

  @Override
  public CompletableFuture<Response> execute() {
    var resultOrContext = getResultOrContext(request);
    if (resultOrContext.getResult() != null) {
      return requestExecutor
          .handleFailFastResponse(request, resultOrContext.getRequestContext(), resultOrContext.getResult())
          .thenApply(ResponseWrapper::getResponse);
    }
    return requestExecutor
        .executeRequest(
            resultOrContext.getBalancedRequest(this.timeoutMultiplier),
            maxTries - triesLeft,
            resultOrContext.getRequestContext()
        )
        .thenApply(this::finishRequest)
        .thenCompose(this::unwrapOrRetry);
  }

  protected abstract ImmediateResultOrPreparedRequest getResultOrContext(Request request);

  private ResponseWrapper finishRequest(ResponseWrapper wrapper) {
    long timeToLastByteMillis = wrapper.getTimeToLastByteMillis();
    updateLeftTriesAndTime((int) timeToLastByteMillis);
    Response response = wrapper.getResponse();
    this.trace.add(new TraceFrame(response.getUri().getHost(), response.getStatusCode(), response.getStatusText()));
    onRequestReceived(wrapper, timeToLastByteMillis);
    return wrapper;
  }

  protected abstract void onRequestReceived(@Nullable ResponseWrapper wrapper, long timeToLastByteMillis);

  private void updateLeftTriesAndTime(int responseTimeMillis) {
    requestTimeLeftMs = requestTimeLeftMs >= responseTimeMillis ? requestTimeLeftMs - responseTimeMillis : 0;
    if (triesLeft > 0) {
      triesLeft--;
    }
  }

  protected CompletableFuture<Response> unwrapOrRetry(ResponseWrapper wrapper) {
    Response response = wrapper.getResponse();
    boolean doRetry = checkRetry(response);

    int triesUsed = maxTries - triesLeft;
    int retriesCount = triesUsed - 1;

    logResponse(response, wrapper.getTimeToLastByteMillis(), retriesCount, doRetry);
    onResponse(wrapper, triesUsed, doRetry);
    if (doRetry) {
      onRetry();
      return execute();
    }
    return completedFuture(response);
  }

  protected abstract void onResponse(ResponseWrapper wrapper, int triesUsed, boolean willFireRetry);

  private boolean checkRetry(Response response) {
    if (triesLeft == 0 || requestTimeLeftMs == 0) {
      return false;
    }
    boolean isIdempotent = forceIdempotence || !HTTP_POST.equals(request.getMethod());
    return checkRetry(response, isIdempotent);
  }

  protected boolean checkRetry(Response response, boolean isIdempotent){
    return retryPolicy.isRetriable(response, isIdempotent);
  }

  protected abstract void onRetry();

  private void logResponse(Response response, long responseTimeMillis, int retriesCount, boolean doRetry) {
    Supplier<String> sizeSupplier = () -> Optional
        .ofNullable(response.getResponseBodyAsBytes())
        .map(body -> String.format(" got %d bytes", body.length))
        .orElse("");
    boolean isServerError = response.getStatusCode() >= 500;
    if (doRetry) {
      String retry = retriesCount > 0 ? String.format(" on retry %s", retriesCount) : "";
      Level level = isServerError ? Level.WARN : Level.INFO;
      LOGGER
          .atLevel(level)
          .setMessage("balanced_request_response: {} {}{}{} in {} millis on {} {}, will retry")
          .addArgument(response::getStatusCode)
          .addArgument(response::getStatusText)
          .addArgument(sizeSupplier)
          .addArgument(retry)
          .addArgument(responseTimeMillis)
          .addArgument(request::getMethod)
          .addArgument(request::getUri)
          .log();
    } else {
      Level level = isServerError || retriesCount > 0 ? Level.WARN : balancingRequestsLogLevel;
      String msgLabel = isServerError ? "balanced_request_final_error" : "balanced_request_final_response";
      LOGGER
          .atLevel(level)
          .setMessage("{}: {} {}{} in {} millis on {} {}, trace: {}")
          .addArgument(msgLabel)
          .addArgument(response::getStatusCode)
          .addArgument(response::getStatusText)
          .addArgument(sizeSupplier)
          .addArgument(responseTimeMillis)
          .addArgument(request::getMethod)
          .addArgument(request::getUri)
          .addArgument(this::getTrace)
          .log();
    }
  }

  private String getTrace() {
    return this.trace.stream().map(TraceFrame::toString).collect(Collectors.joining(" -> "));
  }

  protected static final class TraceFrame {
    private final String address;
    private final int responseCode;
    private final String msg;

    public TraceFrame(String address, int responseCode, String msg) {
      this.address = address;
      this.responseCode = responseCode;
      this.msg = msg;
    }

    public int getResponseCode() {
      return responseCode;
    }

    @Override
    public String toString() {
      return address + "~" + responseCode + "~" + msg;
    }
  }
}
