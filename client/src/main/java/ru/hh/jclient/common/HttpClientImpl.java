package ru.hh.jclient.common;

import static java.lang.Boolean.TRUE;
import java.time.Instant;
import static java.time.Instant.now;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import static java.util.Set.of;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static ru.hh.jclient.common.HttpHeaderNames.ACCEPT;
import static ru.hh.jclient.common.HttpHeaderNames.AUTHORIZATION;
import static ru.hh.jclient.common.HttpHeaderNames.FRONTIK_DEBUG_AUTH;
import static ru.hh.jclient.common.HttpHeaderNames.HH_PROTO_SESSION;
import static ru.hh.jclient.common.HttpHeaderNames.TMS_PROTO_SESSION;
import static ru.hh.jclient.common.HttpHeaderNames.X_HH_ACCEPT_ERRORS;
import static ru.hh.jclient.common.HttpHeaderNames.X_HH_DEBUG;
import static ru.hh.jclient.common.HttpHeaderNames.X_HH_PROFESSIONAL_ROLES_MODE;
import static ru.hh.jclient.common.HttpHeaderNames.X_LOAD_TESTING;
import static ru.hh.jclient.common.HttpHeaderNames.X_REAL_IP;
import static ru.hh.jclient.common.HttpHeaderNames.X_REQUEST_ID;
import static ru.hh.jclient.common.HttpHeaderNames.X_SOURCE;
import static ru.hh.jclient.common.HttpParams.READ_ONLY_REPLICA;
import ru.hh.jclient.common.RequestStrategy.RequestExecutor;
import ru.hh.jclient.common.util.ContentType;
import ru.hh.jclient.common.util.MDCCopy;
import ru.hh.jclient.common.util.storage.Storage;
import ru.hh.jclient.common.util.storage.StorageUtils.Transfers;

class HttpClientImpl extends HttpClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientImpl.class);

  static final Set<String> PASS_THROUGH_HEADERS = of(
      X_REQUEST_ID,
      X_REAL_IP,
      AUTHORIZATION,
      HH_PROTO_SESSION,
      TMS_PROTO_SESSION,
      X_HH_DEBUG,
      FRONTIK_DEBUG_AUTH,
      X_LOAD_TESTING,
      X_SOURCE,
      X_HH_PROFESSIONAL_ROLES_MODE
  );

  private final Executor callbackExecutor;

  HttpClientImpl(
      AsyncHttpClient http,
      Request request,
      RequestStrategy<? extends RequestEngineBuilder<?>> requestStrategy,
      Storage<HttpClientContext> contextSupplier,
      Set<String> customHostsWithSession,
      Executor callbackExecutor,
      List<HttpClientEventListener> eventListeners
  ) {
    super(http, request, requestStrategy, contextSupplier, customHostsWithSession, eventListeners);
    this.callbackExecutor = callbackExecutor;
  }

  @Override
  CompletableFuture<ResponseWrapper> executeRequest(Request originalRequest, int retryCount, RequestContext context) {
    for (HttpClientEventListener check : getEventListeners()) {
      check.beforeExecute(this, originalRequest);
    }

    CompletableFuture<ResponseWrapper> promise = new CompletableFuture<>();

    Request request = addHeadersAndParams(originalRequest, context);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("HTTP_CLIENT_REQUEST: {} ", request.toStringExtended());
    }
    if (retryCount > 0) {
      LOGGER.debug("HTTP_CLIENT_RETRY {}: {} {}", retryCount, request.getMethod(), request.getUri());
      getDebugs().forEach(debug -> debug.onRetry(request, getRequestBodyEntity().orElse(null), retryCount, context));
    } else {
      LOGGER.debug("HTTP_CLIENT_START: Starting {} {}", request.getMethod(), request.getUri());
      getDebugs().forEach(debug -> debug.onRequest(request, getRequestBodyEntity().orElse(null), context));
    }

    Transfers transfers = getStorages().prepare();
    CompletionHandler handler = new CompletionHandler(promise, request, now(), getDebugs(), transfers, callbackExecutor);
    getHttp().executeRequest(request.getDelegate(), handler);

    return promise;
  }

  private Request addHeadersAndParams(Request request, RequestContext context) {
    RequestBuilder requestBuilder = new RequestBuilder(request);

    // compute headers. Headers from context are used as base, with headers from request overriding any existing values
    HttpHeaders headers = new HttpHeaders();
    if (!isExternalRequest()) {
      headers.add(HttpHeaderNames.X_OUTER_TIMEOUT_MS, Integer.toString(request.getRequestTimeout()));
      PASS_THROUGH_HEADERS
          .stream()
          .filter(getContext().getHeaders()::containsKey)
          .forEach(h -> headers.add(h, getContext().getHeaders().get(h)));
    }

    boolean canUnwrapDebugResponse = getDebugs().stream().anyMatch(RequestDebug::canUnwrapDebugResponse);
    boolean enableDebug = !isNoDebug() && !isExternalRequest() && getContext().isDebugMode() && canUnwrapDebugResponse;

    // debug header is passed through by default, but should be removed if debug is not enabled
    if (!enableDebug) {
      headers.remove(X_HH_DEBUG);
    }

    headers.add(request.getHeaders());

    if (isNoSessionRequired(context)) {
      headers.remove(HH_PROTO_SESSION);
      headers.remove(TMS_PROTO_SESSION);
    }

    requestBuilder.setHeaders(headers);

    if (!headers.contains(ACCEPT) && !getExpectedMediaTypes().isEmpty()) {
      requestBuilder.addHeader(ACCEPT, String.join(",", getExpectedMediaTypes()));
    }

    if (!areAllowedMediaTypesForResponseAndErrorCompatible()) {
      LOGGER.warn(
          "Different MediaTypes for successful answer and for errors on {} {} s: {} e: {} ",
          request.getMethod(),
          request.getUri(),
          getExpectedMediaTypes().stream().map(Object::toString).collect(Collectors.joining(",")),
          getExpectedMediaTypesForErrors().stream().map(Object::toString).collect(Collectors.joining(","))
      );
    }

    if (!headers.contains(X_HH_ACCEPT_ERRORS) && !getExpectedMediaTypesForErrors().isEmpty()) {
      requestBuilder.addHeader(
          X_HH_ACCEPT_ERRORS,
          getExpectedMediaTypesForErrors().stream().map(Object::toString).collect(Collectors.joining(","))
      );
    }

    // add readonly param
    if (useReadOnlyReplica() && !isExternalRequest()) {
      requestBuilder.addQueryParam(READ_ONLY_REPLICA, TRUE.toString());
    }

    // add both debug param and debug header (for backward compatibility)
    if (enableDebug && !isExternalRequest()) {
      requestBuilder.setHeader(X_HH_DEBUG, "true");
      requestBuilder.addQueryParam(HttpParams.DEBUG, HttpParams.getDebugValue());
    }

    requestBuilder.setExternalRequest(isExternalRequest());

    // sanity check for debug header/param if debug is not enabled
    if (!enableDebug) {
      if (headers.contains(X_HH_DEBUG)) {
        throw new IllegalStateException("Debug header in request when debug is disabled");
      }

      if (request.getQueryParams().stream().anyMatch(param -> param.getName().equals(HttpParams.DEBUG))) {
        throw new IllegalStateException("Debug param in request when debug is disabled");
      }
    }
    return requestBuilder.build();
  }

  private boolean areAllowedMediaTypesForResponseAndErrorCompatible() {
    if (getExpectedMediaTypes().isEmpty() || getExpectedMediaTypesForErrors().isEmpty()) {
      return true;
    }

    if (getExpectedMediaTypes().size() == 1 && getExpectedMediaTypes().contains(ContentType.ANY)) {
      return true;
    }

    return getExpectedMediaTypes().equals(getExpectedMediaTypesForErrors());
  }

  @Override
  RequestExecutor createRequestExecutor() {
    return new RequestStrategy.RequestExecutor() {
      @Override
      public CompletableFuture<ResponseWrapper> executeRequest(Request request, int retryCount, RequestContext requestContext) {
        if (retryCount > 0) {
          // due to retry possibly performed in another thread
          // TODO do not re-get suppliers here
          setDebugs(getContext().getDebugSuppliers().stream().map(Supplier::get).collect(toList()));
        }
        return HttpClientImpl.this.executeRequest(request, retryCount, requestContext);
      }

      @Override
      public CompletableFuture<ResponseWrapper> handleFailFastResponse(Request request, RequestContext requestContext, Response response) {
        for (RequestDebug requestDebug : getDebugs()) {
          requestDebug.onRequest(request, getRequestBodyEntity().orElse(null), requestContext);
        }
        Transfers transfers = getStorages().prepare();
        CompletableFuture<ResponseWrapper> promise = new CompletableFuture<>();
        HttpClientImpl.proceedWithResponse(response, 0, getDebugs(), transfers, promise, callbackExecutor);
        return promise;
      }

      @Override
      public int getDefaultRequestTimeoutMs() {
        return getHttp().getConfig().getRequestTimeout();
      }
    };
  }

  private static ResponseWrapper proceedWithResponse(
      Response response,
      long responseTimeMillis,
      List<RequestDebug> requestDebugs,
      Transfers contextTransfers,
      CompletableFuture<ResponseWrapper> promise,
      Executor callbackExecutor
  ) {

    for (RequestDebug debug : requestDebugs) {
      response = debug.onResponse(response);
    }
    ResponseWrapper wrapper = new ResponseWrapper(response, responseTimeMillis);
    // complete promise in a separate thread to avoid blocking caller thread
    callbackExecutor.execute(() -> {
      try {
        // install context(s) in current (callback) thread so chained tasks have context to run with
        contextTransfers.perform();
        promise.complete(wrapper);
      } finally {
        // remove context(s) once the promise completes
        contextTransfers.rollback();
      }
    });
    return wrapper;
  }


  static class CompletionHandler extends AsyncCompletionHandler<ResponseWrapper> {
    private final MDCCopy mdcCopy;
    private final CompletableFuture<ResponseWrapper> promise;
    private final Request request;
    private final Instant requestStart;
    private final List<RequestDebug> requestDebugs;
    private final Transfers contextTransfers;
    private final Executor callbackExecutor;

    CompletionHandler(
        CompletableFuture<ResponseWrapper> promise,
        Request request,
        Instant requestStart,
        List<RequestDebug> requestDebugs,
        Transfers contextTransfers,
        Executor callbackExecutor
    ) {
      this.requestStart = requestStart;
      mdcCopy = MDCCopy.capture();
      this.promise = promise;
      this.request = request;
      this.requestDebugs = List.copyOf(requestDebugs);
      this.contextTransfers = contextTransfers;
      this.callbackExecutor = callbackExecutor;
    }

    @Override
    public ResponseWrapper onCompleted(org.asynchttpclient.Response response) {
      int responseStatusCode = response.getStatusCode();
      String responseStatusText = response.getStatusText();

      long timeToLastByteMillis = getTimeToLastByte();
      mdcCopy.doInContext(() -> LOGGER.debug("HTTP_CLIENT_RESPONSE: {} {} in {} millis on {} {}",
          responseStatusCode, responseStatusText, timeToLastByteMillis, request.getMethod(), request.getUri()
      ));

      return proceedWithResponse(response, timeToLastByteMillis);
    }

    @Override
    public void onThrowable(Throwable t) {
      org.asynchttpclient.Response response = TransportExceptionMapper.map(t, request.getUri());
      long timeToLastByteMillis = getTimeToLastByte();

      mdcCopy.doInContext(
          () -> LOGGER.debug(
              "HTTP_CLIENT_ERROR: client error after {} millis on {} {}: {}{}",
              timeToLastByteMillis,
              request.getMethod(),
              request.getUri(),
              t,
              response != null ? " (mapped to " + response.getStatusCode() + "), proceeding" : ", propagating"
          ));

      if (response != null) {
        proceedWithResponse(response, timeToLastByteMillis);
        return;
      }

      requestDebugs.forEach(debug -> debug.onClientProblem(t));
      requestDebugs.forEach(RequestDebug::onProcessingFinished);

      completeExceptionally(t);
    }

    private ResponseWrapper proceedWithResponse(org.asynchttpclient.Response response, long responseTimeMillis) {
      return HttpClientImpl.proceedWithResponse(
          new Response(response),
          responseTimeMillis,
          requestDebugs,
          contextTransfers,
          promise,
          callbackExecutor
      );
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
            LOGGER.error("Failed to complete promise exceptionally in a separate thread: {}, using ning thread", e, e);
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
