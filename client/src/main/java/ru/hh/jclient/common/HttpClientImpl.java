package ru.hh.jclient.common;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;

import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ru.hh.jclient.common.HttpHeaderNames.FRONTIK_DEBUG_AUTH;
import static ru.hh.jclient.common.HttpHeaderNames.HH_PROTO_SESSION;
import static ru.hh.jclient.common.HttpHeaderNames.X_HH_ANALYTIC_DATA;
import static ru.hh.jclient.common.HttpHeaderNames.X_HH_DEBUG;
import static ru.hh.jclient.common.HttpHeaderNames.X_LOAD_TESTING;
import static ru.hh.jclient.common.HttpHeaderNames.X_REAL_IP;
import static ru.hh.jclient.common.HttpHeaderNames.X_REQUEST_ID;
import static ru.hh.jclient.common.HttpHeaderNames.X_SOURCE;
import static ru.hh.jclient.common.HttpParams.READ_ONLY_REPLICA;

import ru.hh.jclient.common.util.MDCCopy;
import ru.hh.jclient.common.util.storage.StorageUtils.Transfers;
import ru.hh.jclient.common.util.storage.Storage;
import static java.time.Instant.now;

class HttpClientImpl extends HttpClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientImpl.class);

  static final Set<String> PASS_THROUGH_HEADERS = of(X_REQUEST_ID, X_REAL_IP, AUTHORIZATION, HH_PROTO_SESSION,
    X_HH_DEBUG, FRONTIK_DEBUG_AUTH, X_LOAD_TESTING, X_SOURCE, X_HH_ANALYTIC_DATA);

  private final Executor callbackExecutor;

  HttpClientImpl(AsyncHttpClient http,
                 Request request,
                 Set<String> hostsWithSession,
                 UpstreamManager upstreamManager,
                 Storage<HttpClientContext> contextSupplier,
                 Executor callbackExecutor,
                 List<HttpClientEventListener> eventListeners) {
    this(http, request, hostsWithSession, upstreamManager, contextSupplier, callbackExecutor, eventListeners, false);
  }

  HttpClientImpl(AsyncHttpClient http,
                 Request request,
                 Set<String> hostsWithSession,
                 UpstreamManager upstreamManager,
                 Storage<HttpClientContext> contextSupplier,
                 Executor callbackExecutor,
                 List<HttpClientEventListener> eventListeners,
                 boolean adaptive) {
    super(http, request, hostsWithSession, upstreamManager, contextSupplier, eventListeners, adaptive);
    this.callbackExecutor = callbackExecutor;
  }

  @Override
  CompletableFuture<ResponseWrapper> executeRequest(Request request, int retryCount, RequestContext context) {
    getEventListeners().forEach(check -> check.beforeExecute(this));

    CompletableFuture<ResponseWrapper> promise = new CompletableFuture<>();

    request = addHeadersAndParams(request);
    if (retryCount > 0) {
      LOGGER.info("ASYNC_HTTP_RETRY {}: {} {}", retryCount, request.getMethod(), request.getUri());
      getDebug().onRetry(request, getRequestBodyEntity(), retryCount, context);
    } else {
      LOGGER.debug("ASYNC_HTTP_START: Starting {} {}", request.getMethod(), request.getUri());
      getDebug().onRequest(request, getRequestBodyEntity(), context);
    }

    Transfers transfers = getStorages().prepare();
    CompletionHandler handler = new CompletionHandler(promise, request, now(), getDebug(), transfers, callbackExecutor);
    getHttp().executeRequest(request.getDelegate(), handler);

    return promise;
  }

  private Request addHeadersAndParams(Request request) {
    RequestBuilder requestBuilder = new RequestBuilder(request);

    // compute headers. Headers from context are used as base, with headers from request overriding any existing values
    HttpHeaders headers = new HttpHeaders();
    if (!isExternalRequest()) {
      PASS_THROUGH_HEADERS.stream()
        .filter(getContext().getHeaders()::containsKey)
        .forEach(h -> headers.add(h, getContext().getHeaders().get(h)));
    }

    // debug header is passed through by default, but should be removed before final check at the end if client specifies so
    if (isNoDebug() || isExternalRequest() || !getContext().isDebugMode()) {
      headers.remove(X_HH_DEBUG);
    }

    headers.add(request.getHeaders());

    if (isNoSessionRequired()) {
      headers.remove(HH_PROTO_SESSION);
    }

    requestBuilder.setHeaders(headers);

    if (!headers.contains(ACCEPT) && getExpectedMediaTypes().isPresent()) {
      requestBuilder.addHeader(ACCEPT, getExpectedMediaTypes().get().stream().map(Object::toString).collect(Collectors.joining(",")));
    }

    // add readonly param
    if (useReadOnlyReplica() && !isExternalRequest()) {
      requestBuilder.addQueryParam(READ_ONLY_REPLICA, TRUE.toString());
    }

    // add both debug param and debug header (for backward compatibility)
    if (getContext().isDebugMode() && !isNoDebug() && !isExternalRequest()) {
      requestBuilder.setHeader(X_HH_DEBUG, "true");
      requestBuilder.addQueryParam(HttpParams.DEBUG, HttpParams.getDebugValue());
    }

    // sanity check for debug header/param if debug is not enabled
    if (isNoDebug() || isExternalRequest() || !getContext().isDebugMode()) {
      if (headers.contains(X_HH_DEBUG)) {
        throw new IllegalStateException("Debug header in request when debug is disabled");
      }

      if (request.getQueryParams().stream().anyMatch(param -> param.getName().equals(HttpParams.DEBUG))) {
        throw new IllegalStateException("Debug param in request when debug is disabled");
      }
    }
    return requestBuilder.build();
  }

  static class CompletionHandler extends AsyncCompletionHandler<ResponseWrapper> {
    private MDCCopy mdcCopy;
    private CompletableFuture<ResponseWrapper> promise;
    private Request request;
    private Instant requestStart;
    private RequestDebug requestDebug;
    private Transfers contextTransfers;
    private final Executor callbackExecutor;

    CompletionHandler(CompletableFuture<ResponseWrapper> promise, Request request, Instant requestStart,
                      RequestDebug requestDebug, Transfers contextTransfers, Executor callbackExecutor) {
      this.requestStart = requestStart;
      this.mdcCopy = MDCCopy.capture();
      this.promise = promise;
      this.request = request;
      this.requestDebug = requestDebug;
      this.contextTransfers = contextTransfers;
      this.callbackExecutor = callbackExecutor;
    }

    @Override
    public ResponseWrapper onCompleted(org.asynchttpclient.Response response) {
      int responseStatusCode = response.getStatusCode();
      String responseStatusText = response.getStatusText();

      long timeToLastByteMs = getTimeToLastByte();
      mdcCopy.doInContext(() -> LOGGER.info("ASYNC_HTTP_RESPONSE: {} {} in {} ms on {} {}",
          responseStatusCode, responseStatusText, timeToLastByteMs, request.getMethod(), request.getUri()));

      return proceedWithResponse(response, timeToLastByteMs);
    }

    @Override
    public void onThrowable(Throwable t) {
      org.asynchttpclient.Response response = TransportExceptionMapper.map(t, request.getUri());
      long timeToLastByteMs = getTimeToLastByte();

      mdcCopy.doInContext(
          () -> LOGGER.warn(
              "ASYNC_HTTP_ERROR: client error after {} ms on {} {}: {}{}",
              timeToLastByteMs,
              request.getMethod(),
              request.getUri(),
              t.toString(),
              response != null ? " (mapped to " + response.getStatusCode() + "), proceeding" : ", propagating"));

      if (response != null) {
        proceedWithResponse(response, timeToLastByteMs);
        return;
      }

      requestDebug.onClientProblem(t);
      requestDebug.onProcessingFinished();

      completeExceptionally(t);
    }

    private ResponseWrapper proceedWithResponse(org.asynchttpclient.Response response, long responseTimeMs) {
      Response debuggedResponse = requestDebug.onResponse(new Response(response));
      ResponseWrapper wrapper = new ResponseWrapper(debuggedResponse, responseTimeMs);
      // complete promise in a separate thread not to block ning thread
      callbackExecutor.execute(() -> {
        try {
          // install context(s) for current (callback) thread so chained tasks have context to run with
          contextTransfers.perform();
          promise.complete(wrapper);
        } finally {
          // remove context(s) once the promise completes
          contextTransfers.rollback();
        }
      });
      return wrapper;
    }

    private void completeExceptionally(Throwable t) {
      Runnable completeExceptionallyTask = () -> {
        try {
          // install context(s) for current (callback) thread so chained tasks have context to run with
          contextTransfers.perform();
          promise.completeExceptionally(t);
        } finally {
          // remove context(s) once the promise completes
          contextTransfers.rollback();
        }
      };
      try {
        // complete promise in a separate thread not to block ning thread
        callbackExecutor.execute(completeExceptionallyTask);
      } catch (RuntimeException e) {
        mdcCopy.doInContext(() -> {
          if (e instanceof RejectedExecutionException) {
            LOGGER.warn("Failed to complete promise exceptionally in a separate thread: {}, using ning thread", e.toString());
          } else {
            LOGGER.error("Failed to complete promise exceptionally in a separate thread: {}, using ning thread", e.toString(), e);
          }
        });
        completeExceptionallyTask.run();
      }
    }

    private long getTimeToLastByte() {
      return requestStart.until(now(), ChronoUnit.MILLIS);
    }
  }
}
