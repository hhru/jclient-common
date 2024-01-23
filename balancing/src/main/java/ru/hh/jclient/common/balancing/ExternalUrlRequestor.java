package ru.hh.jclient.common.balancing;

import java.util.Set;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestContext;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.jclient.common.Uri;

public class ExternalUrlRequestor extends RequestBalancer {
  public static final String DC_FOR_EXTERNAL_REQUESTS = "externalRequest";
  private static final RetryPolicy DEFAULT_RETRY_POLICY = new RetryPolicy();

  private final Set<Monitoring> monitorings;

  public ExternalUrlRequestor(Request request, RequestStrategy.RequestExecutor requestExecutor,
                              int requestTimeoutMs, int maxRequestTimeoutTries, int maxTries,
                              Double timeoutMultiplier, String balancingRequestsLogLevel, boolean forceIdempotence,
                              Set<Monitoring> monitorings
  ) {
    super(request, requestExecutor, requestTimeoutMs, maxRequestTimeoutTries, maxTries, timeoutMultiplier,
        balancingRequestsLogLevel, forceIdempotence);
    this.monitorings = monitorings;
  }

  @Override
  protected ImmediateResultOrPreparedRequest getResultOrContext(Request request) {
    return new ImmediateResultOrPreparedRequest(RequestContext.EMPTY_CONTEXT, request);
  }

  @Override
  protected void onRequestReceived(ResponseWrapper wrapper, long timeToLastByteMillis) {
  }

  @Override
  protected void onResponse(ResponseWrapper wrapper, int triesUsed, boolean willFireRetry) {
    boolean isRequestFinal = !willFireRetry;
    for (Monitoring monitoring : monitorings) {
      int statusCode = wrapper.getResponse().getStatusCode();
      long requestTimeMillis = wrapper.getTimeToLastByteMillis();

      Uri originalUri = request.getUri();
      Uri baseUri = new Uri(originalUri.getScheme(), null, originalUri.getHost(), originalUri.getPort(), null, null);
      String serverAddress = baseUri.toString();

      monitoring.countRequest(serverAddress, DC_FOR_EXTERNAL_REQUESTS, serverAddress, statusCode, requestTimeMillis, isRequestFinal);
      monitoring.countRequestTime(serverAddress, DC_FOR_EXTERNAL_REQUESTS, requestTimeMillis);

      if (isRequestFinal && triesUsed > 1) {
        monitoring.countRetry(serverAddress, DC_FOR_EXTERNAL_REQUESTS, serverAddress, statusCode, trace.get(0).getResponseCode(), triesUsed);
      }
    }
  }

  @Override
  protected boolean checkRetry(Response response, boolean isIdempotent) {
    return DEFAULT_RETRY_POLICY.isRetriable(response, isIdempotent);
  }

  @Override
  protected void onRetry() {
  }
}
