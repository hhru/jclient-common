package ru.hh.jclient.common.balancing;

import jakarta.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestContext;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.jclient.common.Uri;

public class ExternalUrlRequestor extends RequestBalancer {
  public static final String DC_FOR_EXTERNAL_REQUESTS = "externalRequest";
  private static final Logger LOGGER = LoggerFactory.getLogger(ExternalUrlRequestor.class);

  @Nullable
  private final String upstreamName;
  private final Set<Monitoring> monitorings;

  public ExternalUrlRequestor(
      @Nullable Upstream upstream,
      Request request,
      RequestStrategy.RequestExecutor requestExecutor,
      RetryPolicy retryPolicy,
      int requestTimeoutMs,
      int maxRequestTimeoutTries,
      int maxTries,
      Double timeoutMultiplier,
      String balancingRequestsLogLevel,
      boolean forceIdempotence,
      Set<Monitoring> monitorings
  ) {
    super(request, requestExecutor, retryPolicy, requestTimeoutMs, maxRequestTimeoutTries, maxTries, timeoutMultiplier,
        balancingRequestsLogLevel, forceIdempotence
    );
    this.upstreamName = Optional.ofNullable(upstream).map(Upstream::getName).orElse(null);
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
    int statusCode = wrapper.getResponse().getStatusCode();
    long requestTimeMillis = wrapper.getTimeToLastByteMillis();
    Uri originalUri = request.getUri();
    Uri baseUri = new Uri(originalUri.getScheme(), null, originalUri.getHost(), originalUri.getPort(), null, null);
    String serverAddress = baseUri.toString();
    String name = upstreamName != null ? upstreamName : serverAddress;

    for (Monitoring monitoring : monitorings) {
      try {
        monitoring.countRequest(name, DC_FOR_EXTERNAL_REQUESTS, serverAddress, statusCode, requestTimeMillis, isRequestFinal, "externalRequest");
        monitoring.countRequestTime(name, DC_FOR_EXTERNAL_REQUESTS, requestTimeMillis);

        if (isRequestFinal && triesUsed > 1) {
          monitoring.countRetry(name, DC_FOR_EXTERNAL_REQUESTS, serverAddress, statusCode, trace.get(0).getResponseCode(), triesUsed);
        }
      } catch (Exception e) {
        LOGGER.error("Error occurred while sending metrics", e);
      }
    }
  }

  @Override
  protected void onRetry() {
  }
}
