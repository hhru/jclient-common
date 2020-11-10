package ru.hh.jclient.common.balancing;

import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestContext;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.jclient.common.Uri;

import java.util.Set;

public class ExternalUrlRequestor extends RequestBalancer {
  private static final RetryPolicy DEFAULT_RETRY_POLICY = new RetryPolicy();

  private final Set<Monitoring> monitorings;
  private int firstStatusCode;

  public ExternalUrlRequestor(Request request, RequestStrategy.RequestExecutor requestExecutor,
                              int requestTimeoutMs, int maxRequestTimeoutTries, int maxTries,
                              Double timeoutMultiplier, boolean forceIdempotence,
                              Set<Monitoring> monitorings
  ) {
    super(request, requestExecutor, requestTimeoutMs, maxRequestTimeoutTries, maxTries, timeoutMultiplier, forceIdempotence);
    this.monitorings = monitorings;
  }

  @Override
  protected ImmediateResultOrPreparedRequest getResultOrContext(Request request) {
    return new ImmediateResultOrPreparedRequest(RequestContext.EMPTY_CONTEXT, request);
  }

  @Override
  protected void onRequestReceived(ResponseWrapper wrapper, long timeToLastByteMicros) {

  }

  @Override
  protected void onResponse(ResponseWrapper wrapper, int triesUsed, boolean willFireRetry) {
    for (Monitoring monitoring : monitorings) {
      int statusCode = wrapper.getResponse().getStatusCode();
      long requestTimeMicros = wrapper.getTimeToLastByteMicros();

      String serverAddress;
      String dcName = "externalRequest";
      Uri originalUri = request.getUri();
      Uri baseUri = new Uri(originalUri.getScheme(), null, originalUri.getHost(), originalUri.getPort(), null, null);
      serverAddress = baseUri.toString();

      monitoring.countRequest(serverAddress, dcName, serverAddress, statusCode, requestTimeMicros, !willFireRetry);
      monitoring.countRequestTime(null, dcName, requestTimeMicros);

      if (triesUsed > 0) {
        monitoring.countRetry(serverAddress, dcName, serverAddress, statusCode, firstStatusCode, triesUsed);
      }
    }
  }

  @Override
  protected boolean checkRetry(Response response, boolean isIdempotent) {
    return DEFAULT_RETRY_POLICY.isRetriable(response, isIdempotent);
  }

  @Override
  protected void onRetry(int statusCode, Response response, int triesUsed) {
    if (triesUsed == 1) {
      firstStatusCode = statusCode;
    }
  }
}
