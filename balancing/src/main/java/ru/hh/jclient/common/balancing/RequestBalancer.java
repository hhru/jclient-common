package ru.hh.jclient.common.balancing;

import java.util.ArrayList;

import static java.util.concurrent.CompletableFuture.completedFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestEngine;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.Response;

import static ru.hh.jclient.common.JClientBase.HTTP_POST;

import ru.hh.jclient.common.ResponseWrapper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public abstract class RequestBalancer implements RequestEngine {
  private static final Logger LOGGER = LoggerFactory.getLogger(RequestBalancer.class);
  static final int WARM_UP_DEFAULT_TIME_MICROS = 100_000;

  protected final Request request;
  private final RequestStrategy.RequestExecutor requestExecutor;
  protected final int maxTries;
  private final boolean forceIdempotence;
  private final List<TraceFrame> trace;

  private int triesLeft;
  private int requestTimeLeftMs;
  private final double timeoutMultiplier;
  protected int requestTimeoutMs;

  RequestBalancer(Request request,
                  RequestStrategy.RequestExecutor requestExecutor,
                  int requestTimeoutMs,
                  int maxRequestTimeoutTries,
                  int maxTries,
                  @Nullable Double timeoutMultiplier,
                  boolean forceIdempotence
  ) {
    this.timeoutMultiplier = Optional.ofNullable(timeoutMultiplier).orElse(1.0);
    this.request = request;
    this.requestExecutor = requestExecutor;
    this.forceIdempotence = forceIdempotence;
    this.requestTimeoutMs = requestTimeoutMs;

    requestTimeoutMs = (int)((request.getRequestTimeout() > 0 ? request.getRequestTimeout() : requestTimeoutMs) * this.timeoutMultiplier);

    requestTimeLeftMs = (int)(requestTimeoutMs * maxRequestTimeoutTries * this.timeoutMultiplier);
    this.maxTries = maxTries;
    this.trace = new ArrayList<>();

    triesLeft = this.maxTries;
  }

  @Override
  public CompletableFuture<Response> execute() {
    var resultOrContext = getResultOrContext(request);
    if (resultOrContext.getResult() != null) {
      return resultOrContext.getResult();
    }
    return requestExecutor.executeRequest(
      resultOrContext.getBalancedRequest(this.timeoutMultiplier),
      maxTries - triesLeft,
      resultOrContext.getRequestContext()
    )
      .whenComplete((wrapper, throwable) -> finishRequest(wrapper))
      .thenCompose(this::unwrapOrRetry);
  }

  protected abstract ImmediateResultOrPreparedRequest getResultOrContext(Request request);

  private void finishRequest(ResponseWrapper wrapper) {
    long timeToLastByteMicros = WARM_UP_DEFAULT_TIME_MICROS;
    if (wrapper != null) {
      timeToLastByteMicros = wrapper.getTimeToLastByteMicros();
      updateLeftTriesAndTime((int) timeToLastByteMicros);
      Response response = wrapper.getResponse();
      this.trace.add(new TraceFrame(response.getUri().getHost(), response.getStatusCode(), response.getStatusText()));
    }
    onRequestReceived(wrapper, timeToLastByteMicros);
  }
  protected abstract void onRequestReceived(@Nullable ResponseWrapper wrapper, long timeToLastByteMicros);

  private void updateLeftTriesAndTime(int responseTimeMicros) {
    var responseTimeMs = responseTimeMicros / 1000;
    requestTimeLeftMs = requestTimeLeftMs >= responseTimeMs ? requestTimeLeftMs - responseTimeMs: 0;
    if (triesLeft > 0) {
      triesLeft--;
    }
  }

  protected CompletableFuture<Response> unwrapOrRetry(ResponseWrapper wrapper) {
    Response response = wrapper.getResponse();
    boolean doRetry = checkRetry(response);
    int triesUsed = maxTries - triesLeft;
    onResponse(wrapper, triesUsed, doRetry);
    int statusCode = response.getStatusCode();
    if (doRetry) {
      logRetryResponse(statusCode, response);
      onRetry(statusCode, response, triesUsed);
      return execute();
    }
    logFinalResponse(statusCode, response);
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

  protected abstract boolean checkRetry(Response response, boolean isIdempotent);

  protected abstract void onRetry(int statusCode, Response response, int triesUsed);

  private void logFinalResponse(int statusCode, Response response) {
    String messageTemplate = "{}: {} {} on {} {}, trace: {}";
    if (statusCode >= 500) {
      LOGGER.warn(messageTemplate, "balanced_request_final_error", response.getStatusCode(), response.getStatusText(),
        request.getMethod(), request.getUri(), getTrace()
      );
    } else {
      LOGGER.info(messageTemplate, "balanced_request_final_response", response.getStatusCode(), response.getStatusText(),
        request.getMethod(), request.getUri(), getTrace()
      );
    }
  }

  private void logRetryResponse(int statusCode, Response response) {
    String messageTemplate = "balanced_request_response: {} {} on {} {}";
    if (statusCode >= 500) {
      LOGGER.info(messageTemplate, response.getStatusCode(), response.getStatusText(), request.getMethod(), response.getUri());
    } else {
      LOGGER.debug(messageTemplate, response.getStatusCode(), response.getStatusText(), request.getMethod(), response.getUri());
    }
  }

  private String getTrace() {
    return this.trace.stream().map(TraceFrame::toString).collect(Collectors.joining("->"));
  }

  private static final class TraceFrame {
    private final String address;
    private final int responseCode;
    private final String msg;

    public TraceFrame(String address, int responseCode, String msg) {
      this.address = address;
      this.responseCode = responseCode;
      this.msg = msg;
    }

    @Override
    public String toString() {
      return address + "~" + responseCode + "~" + msg;
    }
  }
}
