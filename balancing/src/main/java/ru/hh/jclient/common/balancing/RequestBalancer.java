package ru.hh.jclient.common.balancing;

import java.util.Optional;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.sun.istack.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.MappedTransportErrorResponse;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.RequestContext;
import ru.hh.jclient.common.RequestEngine;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResponseConverterUtils;

import static ru.hh.jclient.common.HttpStatuses.BAD_GATEWAY;
import static ru.hh.jclient.common.JClientBase.HTTP_POST;
import static ru.hh.jclient.common.balancing.AdaptiveBalancingStrategy.WARM_UP_DEFAULT_TIME_MICROS;

import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.jclient.common.Uri;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static ru.hh.jclient.common.balancing.BalancingUpstreamManager.SCHEMA_SEPARATOR;

public class RequestBalancer implements RequestEngine {
  private static final Logger logger = LoggerFactory.getLogger(RequestBalancer.class);

  private final Request request;
  private final Upstream upstream;
  private final UpstreamManager upstreamManager;
  private final RequestStrategy.RequestExecutor requestExecutor;
  private final Set<Integer> triedServers = new HashSet<>();
  private final List<Server> servers;
  private final int maxTries;
  private final boolean adaptive;
  private final boolean forceIdempotence;

  private ServerEntry currentServer;
  private int triesLeft;
  private int requestTimeLeftMs;
  private int firstStatusCode;
  private Iterator<ServerEntry> serverEntryIterator;
  private String upstreamName;
  private boolean adaptiveFailed;

  RequestBalancer(Request request,
                         UpstreamManager upstreamManager,
                         RequestStrategy.RequestExecutor requestExecutor,
                         Integer maxRequestTimeoutTries,
                         List<Server> servers,
                         boolean forceIdempotence,
                         boolean adaptive,
                         @Nullable String profile) {
    this.request = request;
    this.upstreamManager = upstreamManager;
    this.requestExecutor = requestExecutor;
    this.servers = servers;
    this.adaptive = adaptive;
    this.forceIdempotence = forceIdempotence;
    String host = request.getUri().getHost();
    upstream = upstreamManager.getUpstream(host, profile);
    upstreamName = upstream == null ? null : upstream.getName();
    int requestTimeoutMs = request.getRequestTimeout() > 0 ? request.getRequestTimeout() :
      upstream != null ? upstream.getConfig().getRequestTimeoutMs() : requestExecutor.getDefaultRequestTimeoutMs();

    int requestTimeoutTries = maxRequestTimeoutTries != null ? maxRequestTimeoutTries :
      upstream != null ? upstream.getConfig().getMaxTimeoutTries() : UpstreamConfig.DEFAULT_MAX_TIMEOUT_TRIES;
    requestTimeLeftMs = requestTimeoutMs * requestTimeoutTries;
    maxTries = upstream != null ? upstream.getConfig().getMaxTries() : UpstreamConfig.DEFAULT_MAX_TRIES;

    triesLeft = upstream != null ? upstream.getConfig().getMaxTries() : UpstreamConfig.DEFAULT_MAX_TRIES;
  }

  @Override
  public CompletableFuture<Response> execute() {
    Request balancedRequest = request;
    RequestContext context = RequestContext.EMPTY_CONTEXT;
    if (isUpstreamAvailable()) {
      balancedRequest = getBalancedRequest(request);
      if (!isServerAvailable()) {
        return completedFuture(getServerNotAvailableResponse(request, upstreamName));
      }
      context = new RequestContext(upstreamName, Optional.ofNullable(currentServer.getDatacenter()).map(String::toLowerCase).orElse(null));
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
      return execute();
    }
    return completedFuture(response);
  }

  private void countStatistics(ResponseWrapper wrapper, boolean doRetry) {
    Set<Monitoring> monitoringSet = upstreamManager.getMonitoring();
    for (Monitoring monitoring : monitoringSet) {
      int statusCode = wrapper.getResponse().getStatusCode();
      long requestTimeMicros = wrapper.getTimeToLastByteMicros();

      String serverAddress;
      String dcName = null;
      if (isServerAvailable()) {
        serverAddress = currentServer.getAddress();
        dcName = currentServer.getDatacenter();
      } else {
        Uri originalUri = request.getUri();
        Uri baseUri = new Uri(originalUri.getScheme(), null, originalUri.getHost(), originalUri.getPort(), null, null);
        serverAddress = baseUri.toString();
        upstreamName = upstreamName == null ? serverAddress : upstreamName;
      }

      monitoring.countRequest(upstreamName, dcName, serverAddress, statusCode, requestTimeMicros, !doRetry);
      monitoring.countRequestTime(upstreamName, dcName, requestTimeMicros);

      if (!triedServers.isEmpty()) {
        monitoring.countRetry(upstreamName, dcName, serverAddress, statusCode, firstStatusCode, triedServers.size());
      }
    }
  }

  private static Response getServerNotAvailableResponse(Request request, String upstreamName) {
    Uri uri = request.getUri();
    return ResponseConverterUtils.convert(
        new MappedTransportErrorResponse(BAD_GATEWAY, "No available servers for upstream: " + upstreamName, uri)
    );
  }

  private Request getBalancedRequest(Request request) {
    if (adaptive && !adaptiveFailed) {
      try {
        currentServer = acquireAdaptiveServer();
      } catch (RuntimeException e) {
        logger.error("failed to acquire adaptive servers", e);
        adaptiveFailed = true;
        currentServer = upstream.acquireServer(triedServers, servers);
      }
    } else {
      currentServer = upstream.acquireServer(triedServers, servers);
    }
    if (currentServer == null) {
      return request;
    }

    int requestTimeout = request.getRequestTimeout() > 0 ? request.getRequestTimeout()
        : upstream.getConfig().getRequestTimeoutMs();

    RequestBuilder requestBuilder = new RequestBuilder(request);
    requestBuilder.setUrl(getBalancedUrl(request, currentServer.getAddress()));
    requestBuilder.setRequestTimeout((int) (requestTimeout * upstreamManager.getTimeoutMultiplier()));
    return requestBuilder.build();
  }

  private ServerEntry acquireAdaptiveServer() {
    if (serverEntryIterator == null) {
      List<ServerEntry> entries = upstream.acquireAdaptiveServers(maxTries, servers);
      serverEntryIterator = entries.iterator();
    }

    return serverEntryIterator.next();
  }

  private void finishRequest(ResponseWrapper wrapper) {
    long timeToLastByteMicros = WARM_UP_DEFAULT_TIME_MICROS;
    if (wrapper != null) {
      timeToLastByteMicros = wrapper.getTimeToLastByteMicros();
      updateLeftTriesAndTime((int) timeToLastByteMicros);
    }

    if (isServerAvailable()) {
      boolean isError = wrapper != null && upstream.getConfig().getRetryPolicy().isServerError(wrapper.getResponse());
      upstream.releaseServer(currentServer.getIndex(), isError, timeToLastByteMicros,
              adaptive && !adaptiveFailed, servers);
    }
  }

  private void updateLeftTriesAndTime(int responseTimeMicros) {
    var responseTimeMs = responseTimeMicros / 1000;
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
    boolean isIdempotent = forceIdempotence || !HTTP_POST.equals(request.getMethod());
    return upstream.getConfig().getRetryPolicy().isRetriable(response, isIdempotent);
  }

  private boolean isServerAvailable() {
    return currentServer != null && currentServer.getIndex() >= 0;
  }

  private boolean isUpstreamAvailable() {
    return upstream != null && upstream.isEnabled();
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
}
