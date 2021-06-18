package ru.hh.jclient.common.balancing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.MappedTransportErrorResponse;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.RequestContext;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResponseConverterUtils;
import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.jclient.common.Uri;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

import static ru.hh.jclient.common.HttpStatuses.BAD_GATEWAY;
import static ru.hh.jclient.common.balancing.BalancingUpstreamManager.SCHEMA_SEPARATOR;

public class UpstreamRequestBalancer extends RequestBalancer {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamRequestBalancer.class);

  private final BalancingState state;

  private final Set<Monitoring> monitorings;
  private int firstStatusCode;

  public UpstreamRequestBalancer(BalancingState state, Request request, RequestStrategy.RequestExecutor requestExecutor,
                                 int maxTimeoutTries, boolean forceIdempotence,
                                 @Nullable Double timeoutMultiplier, Set<Monitoring> monitorings) {
    super(request, requestExecutor,
      state.getUpstreamConfig().getRequestTimeoutMs(),
      maxTimeoutTries,
      state.getUpstreamConfig().getMaxTries(),
      timeoutMultiplier, forceIdempotence
    );
    this.state = state;
    this.monitorings = monitorings;
  }

  @Override
  protected ImmediateResultOrPreparedRequest getResultOrContext(Request request) {
    String upstreamName = state.getUpstreamName();
    if (!state.isUpstreamEnabled()) {
      LOGGER.warn("Upstream {} is disabled. Returning serverNotAvailableResponse", upstreamName);
      return new ImmediateResultOrPreparedRequest(getServerNotAvailableResponse(request, upstreamName), new RequestContext(upstreamName, "unknown"));
    }
    state.acquireServer();
    if (!state.isServerAvailable()) {
      return new ImmediateResultOrPreparedRequest(getServerNotAvailableResponse(request, upstreamName), new RequestContext(upstreamName, "unknown"));
    }
    int requestTimeout = request.getRequestTimeout() > 0 ? request.getRequestTimeout()
      : state.getUpstreamConfig().getRequestTimeoutMs();

    RequestBuilder requestBuilder = new RequestBuilder(request);
    requestBuilder.setUrl(getBalancedUrl(request, state.getCurrentServer().getAddress()));
    requestBuilder.setRequestTimeout(requestTimeout);
    String dc = Optional.ofNullable(state.getCurrentServer().getDatacenter()).map(String::toLowerCase).orElse(null);
    var context = new RequestContext(upstreamName, dc);
    return new ImmediateResultOrPreparedRequest(context, requestBuilder.build());
  }

  private static String getBalancedUrl(Request request, String serverAddress) {
    String originalServer = getOriginalServer(request);
    return request.getUrl().replace(originalServer, serverAddress);
  }

  private static String getOriginalServer(Request request) {
    Uri uri = request.getUri();
    var baseUri = uri.getScheme() + SCHEMA_SEPARATOR + uri.getHost() + ":" + uri.getPort();
    return uri.getPort() > -1 ? baseUri : baseUri.substring(0, baseUri.lastIndexOf(":"));
  }

  public static Response getServerNotAvailableResponse(Request request, String upstreamName) {
    Uri uri = request.getUri();
    return ResponseConverterUtils.convert(
      new MappedTransportErrorResponse(BAD_GATEWAY, "No available servers for upstream: " + upstreamName, uri)
    );
  }

  @Override
  protected void onRequestReceived(ResponseWrapper wrapper, long timeToLastByteMicros) {
    state.releaseServer(timeToLastByteMicros, isServerError(wrapper));
  }

  @Override
  protected void onResponse(ResponseWrapper wrapper, int triesUsed, boolean willFireRetry) {
    for (Monitoring monitoring : monitorings) {
      int statusCode = wrapper.getResponse().getStatusCode();
      long requestTimeMicros = wrapper.getTimeToLastByteMicros();

      if (!state.isServerAvailable()) {
        LOGGER.warn("Monitoring won't be sent", new IllegalStateException("Got response, but server is not available"));
        return;
      }
      var serverAddress = state.getCurrentServer().getAddress();
      var dcName = state.getCurrentServer().getDatacenter();
      String upstreamName = state.getUpstreamName();
      monitoring.countRequest(upstreamName, dcName, serverAddress, statusCode, requestTimeMicros, !willFireRetry);
      monitoring.countRequestTime(upstreamName, dcName, requestTimeMicros);

      if (triesUsed > 1) {
        monitoring.countRetry(upstreamName, dcName, serverAddress, statusCode, firstStatusCode, triesUsed);
      }
    }
  }

  @Override
  protected boolean checkRetry(Response response, boolean isIdempotent) {
    return state.getUpstreamConfig().getRetryPolicy().isRetriable(response, isIdempotent);
  }

  @Override
  protected void onRetry(int statusCode, Response response, int triesUsed) {
    if (triesUsed == 1) {
      firstStatusCode = response.getStatusCode();
    }
    state.incrementTries();
  }

  public boolean isServerError(ResponseWrapper wrapper) {
    return wrapper != null && state.getUpstreamConfig().getRetryPolicy().isServerError(wrapper.getResponse());
  }
}
