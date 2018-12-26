package ru.hh.jclient.common.balancing;

import static java.util.concurrent.CompletableFuture.completedFuture;
import ru.hh.jclient.common.MappedTransportErrorResponse;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.RequestContext;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResponseConverterUtils;

import static ru.hh.jclient.common.JClientBase.HTTP_POST;
import static ru.hh.jclient.common.balancing.AdaptiveBalancingStrategy.WARM_UP_DEFAULT_TIME_MS;

import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.jclient.common.UpstreamManager;
import ru.hh.jclient.common.Uri;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class RequestBalancer {
  private final String host;
  private final Request request;
  private final Upstream upstream;
  private final UpstreamManager upstreamManager;
  private final RequestExecutor requestExecutor;
  private final Set<Integer> triedServers = new HashSet<>();
  private final int maxTries;
  private final boolean adaptive;

  private ServerEntry currentServer = null;
  private int triesLeft;
  private long requestTimeLeftMs;
  private int firstStatusCode;
  private boolean forceIdempotence;
  private Iterator<ServerEntry> serverEntryIterator;

  public RequestBalancer(Request request,
                         UpstreamManager upstreamManager,
                         RequestExecutor requestExecutor,
                         Integer maxRequestTimeoutTries,
                         boolean forceIdempotence,
                         boolean adaptive) {
    this.request = request;
    this.upstreamManager = upstreamManager;
    this.requestExecutor = requestExecutor;
    this.adaptive = adaptive;
    this.forceIdempotence = forceIdempotence;

    host = request.getUri().getHost();
    upstream = upstreamManager.getUpstream(host);
    int requestTimeoutMs = request.getRequestTimeout() > 0 ? request.getRequestTimeout() :
      upstream != null ? upstream.getConfig().getRequestTimeoutMs() : UpstreamConfig.DEFAULT_REQUEST_TIMEOUT_MS;
    int requestTimeoutTries = maxRequestTimeoutTries != null ? maxRequestTimeoutTries :
      upstream != null ? upstream.getConfig().getMaxTimeoutTries() : UpstreamConfig.DEFAULT_MAX_TIMEOUT_TRIES;
    requestTimeLeftMs = requestTimeoutMs * requestTimeoutTries;
    maxTries = upstream != null ? upstream.getConfig().getMaxTries() : UpstreamConfig.DEFAULT_MAX_TRIES;

    triesLeft = upstream != null ? upstream.getConfig().getMaxTries() : UpstreamConfig.DEFAULT_MAX_TRIES;
  }

  public CompletableFuture<Response> requestWithRetry() {
    Request balancedRequest = request;
    RequestContext context = RequestContext.EMPTY_CONTEXT;
    if (isUpstreamAvailable()) {
      balancedRequest = getBalancedRequest(request);
      if (!isServerAvailable()) {
        return completedFuture(getServerNotAvailableResponse(request, upstream.getName()));
      }
      context = new RequestContext(upstream.getName(), currentServer.getRack(), currentServer.getDatacenter());
    }

    return requestExecutor.executeRequest(balancedRequest, triedServers.size(), context)
        .whenComplete((wrapper, throwable) -> finishRequest(wrapper))
        .thenCompose(this::unwrapOrRetry);
  }

  private CompletableFuture<Response> unwrapOrRetry(ResponseWrapper wrapper) {
    boolean doRetry = checkRetry(wrapper.getResponse());
    countStatistics(wrapper, doRetry);
    Response response = wrapper.getResponse();
    if (doRetry) {
      if (triedServers.isEmpty()) {
        firstStatusCode = response.getStatusCode();
      }
      if (isServerAvailable()) {
        triedServers.add(currentServer.getIndex());
        currentServer = null;
      }
      return requestWithRetry();
    }
    return completedFuture(response);
  }

  private void countStatistics(ResponseWrapper wrapper, boolean doRetry) {
    if (isServerAvailable()) {
      Monitoring monitoring = upstreamManager.getMonitoring();
      int statusCode = wrapper.getResponse().getStatusCode();
      monitoring.countRequest(upstream.getName(), currentServer.getDatacenter(), currentServer.getAddress(), statusCode, !doRetry);

      long requestTimeMs = wrapper.getTimeToLastByteMs();
      monitoring.countRequestTime(upstream.getName(), currentServer.getDatacenter(), requestTimeMs);

      if (!triedServers.isEmpty()) {
        monitoring.countRetry(upstream.getName(), currentServer.getDatacenter(), currentServer.getAddress(), statusCode, firstStatusCode, triedServers.size());
      }
    }
  }

  private static Response getServerNotAvailableResponse(Request request, String upstreamName) {
    Uri uri = request.getUri();
    return ResponseConverterUtils.convert(new MappedTransportErrorResponse(502, "No available servers for upstream: " + upstreamName, uri));
  }

  private Request getBalancedRequest(Request request) {
    currentServer = adaptive ? acquireAdaptiveServer() : upstream.acquireServer(triedServers);
    if (currentServer == null) {
      return request;
    }

    int requestTimeout = request.getRequestTimeout() > 0 ? request.getRequestTimeout() : upstream.getConfig().getRequestTimeoutMs();

    RequestBuilder requestBuilder = new RequestBuilder(request);
    requestBuilder.setUrl(getBalancedUrl(request, currentServer.getAddress()));
    requestBuilder.setRequestTimeout((int) (requestTimeout * upstreamManager.getTimeoutMultiplier()));
    return requestBuilder.build();
  }

  private ServerEntry acquireAdaptiveServer() {
    if (serverEntryIterator == null) {
      List<ServerEntry> entries = upstream.acquireAdaptiveServers(maxTries);
      serverEntryIterator = entries.iterator();
    }

    return serverEntryIterator.next();
  }

  private void finishRequest(ResponseWrapper wrapper) {
    long timeToLastByteMs = WARM_UP_DEFAULT_TIME_MS;
    if (wrapper != null) {
      timeToLastByteMs = wrapper.getTimeToLastByteMs();
      updateLeftTriesAndTime(timeToLastByteMs);
    }

    if (isServerAvailable()) {
      boolean isError = wrapper != null && upstream.getConfig().getRetryPolicy().isServerError(wrapper.getResponse());
      upstream.releaseServer(currentServer.getIndex(), isError, timeToLastByteMs, adaptive);
    }
  }

  private void updateLeftTriesAndTime(long responseTimeMs) {
    requestTimeLeftMs = requestTimeLeftMs >= responseTimeMs ? requestTimeLeftMs - responseTimeMs: 0;
    if (triesLeft > 0) {
      triesLeft--;
    }
  }

  private boolean checkRetry(Response response) {
    if (!isUpstreamAvailable()) {
      return false;
    }
    if (triesLeft == 0 || requestTimeLeftMs == 0) {
      return false;
    }
    return upstream.getConfig().getRetryPolicy().isRetriable(response, forceIdempotence || !HTTP_POST.equals(request.getMethod()));
  }

  private boolean isServerAvailable() {
    return currentServer != null && currentServer.getIndex() >= 0;
  }

  private boolean isUpstreamAvailable() {
    return upstream != null;
  }

  private static String getBalancedUrl(Request request, String serverAddress) {
    String originalServer = getOriginalServer(request);
    return request.getUrl().replace(originalServer, serverAddress);
  }

  private static String getOriginalServer(Request request) {
    Uri uri = request.getUri();
    return String.format("%s://%s%s", uri.getScheme(), uri.getHost(), uri.getPort() > -1 ? ":" + uri.getPort() : "");
  }

  @FunctionalInterface
  public interface RequestExecutor {
    CompletableFuture<ResponseWrapper> executeRequest(Request request, int retryCount, RequestContext context);
  }
}
