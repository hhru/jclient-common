package ru.hh.jclient.common.balancing;

import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.uri.Uri;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static ru.hh.jclient.common.AbstractClient.HTTP_POST;
import ru.hh.jclient.common.MappedTransportErrorResponse;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_CONNECT_TIMEOUT;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_REQUEST_TIMEOUT;
import ru.hh.jclient.common.ResponseWrapper;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class RequestBalancer {
  private final String host;
  private final Request request;
  private final Upstream upstream;
  private final Set<Integer> triedServers = new HashSet<>();
  private final RequestExecutor requestExecutor;

  private int triesLeft;
  private long requestTimeLeftMs;
  private int currentServerIndex = -1;

  public RequestBalancer(Request request, UpstreamManager upstreamManager, RequestExecutor requestExecutor) {
    this.request = request;
    this.requestExecutor = requestExecutor;

    host = request.getUri().getHost();
    upstream = upstreamManager.getUpstream(host);
    if (upstream != null) {
      triesLeft = upstream.getConfig().getMaxTries();
      long requestTimeoutMs = upstream.getConfig().getRequestTimeoutMs();
      int maxRequestTimeoutTries = upstream.getConfig().getMaxTimeoutTries();
      requestTimeLeftMs = requestTimeoutMs * maxRequestTimeoutTries;
    } else {
      triesLeft = UpstreamConfig.DEFAULT_MAX_TRIES;
      requestTimeLeftMs = UpstreamConfig.DEFAULT_REQUEST_TIMEOUT_MS * UpstreamConfig.DEFAULT_MAX_TIMEOUT_TRIES;
    }
  }

  public CompletableFuture<Response> requestWithRetry(int retryCount) {
    Request balancedRequest = request;
    if (isUpstreamAvailable()) {
      balancedRequest = getBalancedRequest(request);
      if (!isServerAvailable()) {
        return completedFuture(getServerNotAvailableResponse(request, upstream.getName()));
      }
    }
    return requestExecutor.executeRequest(balancedRequest, retryCount, isUpstreamAvailable() ? upstream.getName() : host)
        .whenComplete((wrapper, throwable) -> finishRequest(wrapper))
        .thenCompose(wrapper -> unwrapOrRetry(wrapper.getResponse(), retryCount));
  }

  private CompletableFuture<Response> unwrapOrRetry(Response response, int retryCount) {
    if (checkRetry(response)) {
      prepareRetry();
      return requestWithRetry(retryCount + 1);
    }
    return completedFuture(response);
  }

  private static Response getServerNotAvailableResponse(Request request, String upstreamName) {
    Uri uri = request.getUri();
    return new MappedTransportErrorResponse(502, "No available servers for upstream: " + upstreamName, uri);
  }

  private Request getBalancedRequest(Request request) {
    RequestBuilder requestBuilder = new RequestBuilder(request);
    int index = upstream.acquireServer(triedServers);
    if (index >= 0) {
      requestBuilder.setUrl(getBalancedUrl(request, upstream, index));
    }
    currentServerIndex = index;
    requestBuilder.setRequestTimeout(upstream.getConfig().getRequestTimeoutMs());
    return requestBuilder.build();
  }

  private void finishRequest(ResponseWrapper wrapper) {
    if (wrapper == null) {
      releaseServer(false);
    } else {
      updateLeftTriesAndTime(wrapper.getTimeToLastByteMs());
      releaseServer(isServerError(wrapper.getResponse()));
    }
  }

  private void updateLeftTriesAndTime(long responseTimeMs) {
    requestTimeLeftMs = requestTimeLeftMs >= responseTimeMs ? requestTimeLeftMs - responseTimeMs: 0;
    if (triesLeft > 0) {
      triesLeft--;
    }
  }

  private void releaseServer(boolean isError) {
    if (isServerAvailable()) {
      upstream.releaseServer(currentServerIndex, isError);
    }
  }

  private boolean checkRetry(Response response) {
    if (!isUpstreamAvailable()) {
      return false;
    }
    boolean isServerError = isServerError(response);
    if (triesLeft == 0 || requestTimeLeftMs == 0 || !isServerError) {
      return false;
    }
    String statusText = response.getStatusText();
    int statusCode = response.getStatusCode();
    boolean isConnectTimeout = statusCode == STATUS_CONNECT_TIMEOUT && (statusText != null && statusText.contains("TimeoutException"));
    boolean isRequestTimeout = statusCode == STATUS_REQUEST_TIMEOUT;
    return isConnectTimeout || (isIdempotent() && isRequestTimeout);
  }

  private void prepareRetry() {
    if (isServerAvailable()) {
      triedServers.add(currentServerIndex);
      currentServerIndex = -1;
    }
  }

  private boolean isServerAvailable() {
    return currentServerIndex >= 0;
  }

  private boolean isUpstreamAvailable() {
    return upstream != null;
  }

  private static boolean isServerError(Response response) {
    int statusCode = response.getStatusCode();
    return statusCode == STATUS_CONNECT_TIMEOUT || statusCode == STATUS_REQUEST_TIMEOUT;
  }

  private boolean isIdempotent() {
    return ! HTTP_POST.equals(request.getMethod());
  }

  private static String getBalancedUrl(Request request, Upstream upstream, int serverIndex) {
    String originalServer = getOriginalServer(request);
    return request.getUrl().replace(originalServer, upstream.getServerAddress(serverIndex));
  }

  private static String getOriginalServer(Request request) {
    Uri uri = request.getUri();
    return String.format("%s://%s%s", uri.getScheme(), uri.getHost(), uri.getPort() > -1 ? ":" + uri.getPort() : "");
  }

  @FunctionalInterface
  public interface RequestExecutor {
    CompletableFuture<ResponseWrapper> executeRequest(Request request, int retryCount, String upstreamName);
  }
}
