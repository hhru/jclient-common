package ru.hh.jclient.common;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;

import com.google.common.net.MediaType;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ru.hh.jclient.common.HttpHeaderNames.X_HH_ACCEPT_ERRORS;
import static ru.hh.jclient.common.HttpHeaderNames.FRONTIK_DEBUG_AUTH;
import static ru.hh.jclient.common.HttpHeaderNames.HH_PROTO_SESSION;
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
  private static final TextMapPropagator.Getter<HttpClientContext> GETTER = createGetter();

  static final Set<String> PASS_THROUGH_HEADERS = of(X_REQUEST_ID, X_REAL_IP, AUTHORIZATION, HH_PROTO_SESSION,
    X_HH_DEBUG, FRONTIK_DEBUG_AUTH, X_LOAD_TESTING, X_SOURCE);

  private final Executor callbackExecutor;
  private final TextMapPropagator textMapPropagator;
  private final Tracer tracer;

  HttpClientImpl(AsyncHttpClient http,
                 Request request,
                 Set<String> hostsWithSession,
                 RequestStrategy<? extends RequestEngineBuilder> requestStrategy,
                 Storage<HttpClientContext> contextSupplier,
                 Executor callbackExecutor,
                 List<HttpClientEventListener> eventListeners,
                 OpenTelemetry openTelemetry) {
    super(http, request, hostsWithSession, requestStrategy, contextSupplier, eventListeners);
    this.callbackExecutor = callbackExecutor;
    this.textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
    this.tracer = openTelemetry.getTracer("jclient");
  }

  @Override
  CompletableFuture<ResponseWrapper> executeRequest(Request originalRequest, int retryCount, RequestContext context) {
    for (HttpClientEventListener check : getEventListeners()) {
      check.beforeExecute(this, originalRequest);
    }
    Span span = startSpan(originalRequest);

    CompletableFuture<ResponseWrapper> promise = new CompletableFuture<>();

    Request request = addHeadersAndParams(originalRequest, span);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("HTTP_CLIENT_REQUEST: {} ", request.toStringExtended());
    }
    if (retryCount > 0) {
      LOGGER.info("HTTP_CLIENT_RETRY {}: {} {}", retryCount, request.getMethod(), request.getUri());
      getDebugs().forEach(debug -> debug.onRetry(request, getRequestBodyEntity(), retryCount, context));
    } else {
      LOGGER.debug("HTTP_CLIENT_START: Starting {} {}", request.getMethod(), request.getUri());
      getDebugs().forEach(debug -> debug.onRequest(request, getRequestBodyEntity(), context));
    }

    Transfers transfers = getStorages().prepare();
    CompletionHandler handler = new CompletionHandler(promise, request, now(), getDebugs(), transfers, callbackExecutor, span);
    getHttp().executeRequest(request.getDelegate(), handler);

    return promise;
  }

  private Request addHeadersAndParams(Request request, Span span) {
    RequestBuilder requestBuilder = new RequestBuilder(request);

    // compute headers. Headers from context are used as base, with headers from request overriding any existing values
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaderNames.X_OUTER_TIMEOUT_MS, Integer.toString(request.getRequestTimeout()));
    try (Scope ignore = span.makeCurrent()) {
      textMapPropagator.inject(Context.current(), headers, HttpHeaders::add);
    }
    if (!isExternalRequest()) {
      PASS_THROUGH_HEADERS.stream()
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

    if (isNoSessionRequired()) {
      headers.remove(HH_PROTO_SESSION);
    }

    requestBuilder.setHeaders(headers);

    if (!headers.contains(ACCEPT) && getExpectedMediaTypes().isPresent()) {
      requestBuilder.addHeader(ACCEPT, getExpectedMediaTypes().get().stream().map(Object::toString).collect(Collectors.joining(",")));
    }

    if (!areAllowedMediaTypesForResponseAndErrorCompatible()) {
      LOGGER.warn("Different MediaTypes for successful answer and for errors on {} {} s: {} e: {} ",
          request.getMethod(),
          request.getUri(),
          getExpectedMediaTypes().get().stream().map(Object::toString).collect(Collectors.joining(",")),
          getExpectedMediaTypesForErrors().get().stream().map(Object::toString).collect(Collectors.joining(","))
      );
    }

    if (!headers.contains(X_HH_ACCEPT_ERRORS) && getExpectedMediaTypesForErrors().isPresent()) {
      requestBuilder.addHeader(
          X_HH_ACCEPT_ERRORS,
          getExpectedMediaTypesForErrors().get().stream().map(Object::toString).collect(Collectors.joining(","))
      );
    }

    // add readonly param
    if (useReadOnlyReplica() && !isExternalRequest()) {
      requestBuilder.addQueryParam(READ_ONLY_REPLICA, TRUE.toString());
    }

    // add both debug param and debug header (for backward compatibility)
    if (enableDebug) {
      requestBuilder.setHeader(X_HH_DEBUG, "true");
      requestBuilder.addQueryParam(HttpParams.DEBUG, HttpParams.getDebugValue());
    }

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

  private Span startSpan(Request originalRequest) {

    Context telemetryContext = textMapPropagator.extract(Context.current(), getContext(), GETTER);
    Span span = tracer.spanBuilder(
        originalRequest.getUrl()) //todo более общий
        .setParent(telemetryContext)
        .setSpanKind(SpanKind.CLIENT)
        .setAttribute("requestTimeout", originalRequest.getRequestTimeout())
        .setAttribute("readTimeout", originalRequest.getReadTimeout())
        .startSpan();
    LOGGER.trace("spanStarted : {}", span);
    return span;
  }

  private boolean areAllowedMediaTypesForResponseAndErrorCompatible() {
    if (getExpectedMediaTypes().isEmpty() || getExpectedMediaTypesForErrors().isEmpty()) {
      return true;
    }

    if (getExpectedMediaTypes().get().size() == 1 && getExpectedMediaTypes().get().contains(MediaType.ANY_TYPE)) {
      return true;
    }

    return getExpectedMediaTypes().get().equals(getExpectedMediaTypesForErrors().get());
  }

  private static TextMapPropagator.Getter<HttpClientContext> createGetter() {
    return new TextMapPropagator.Getter<>() {
      @Override
      public Iterable<String> keys(HttpClientContext carrier) {
        return carrier.getHeaders().keySet();
      }

      @Override
      public String get(HttpClientContext carrier, String key) {
        List<String> header = carrier.getHeaders().get(key);
        if (header == null || header.isEmpty()) {
          return "";
        }
        return header.get(0);
      }
    };
  }

  static class CompletionHandler extends AsyncCompletionHandler<ResponseWrapper> {
    private final MDCCopy mdcCopy;
    private final CompletableFuture<ResponseWrapper> promise;
    private final Request request;
    private final Instant requestStart;
    private final List<RequestDebug> requestDebugs;
    private final Transfers contextTransfers;
    private final Executor callbackExecutor;
    private final Span span;

    CompletionHandler(CompletableFuture<ResponseWrapper> promise, Request request, Instant requestStart,
                      List<RequestDebug> requestDebugs, Transfers contextTransfers, Executor callbackExecutor, Span span) {
      this.requestStart = requestStart;
      mdcCopy = MDCCopy.capture();
      this.promise = promise;
      this.request = request;
      this.requestDebugs = List.copyOf(requestDebugs);
      this.contextTransfers = contextTransfers;
      this.callbackExecutor = callbackExecutor;
      this.span = span;
    }

    @Override
    public ResponseWrapper onCompleted(org.asynchttpclient.Response response) {
      int responseStatusCode = response.getStatusCode();
      String responseStatusText = response.getStatusText();

      long timeToLastByteMicros = getTimeToLastByte();
      mdcCopy.doInContext(() -> LOGGER.info("HTTP_CLIENT_RESPONSE: {} {} in {} micros on {} {}",
          responseStatusCode, responseStatusText, timeToLastByteMicros, request.getMethod(), request.getUri()));
      span.setStatus(StatusCode.OK, String.format("code:%d; description:%s", responseStatusCode, responseStatusText));
      span.end();
      LOGGER.trace("span closed: {}", span);
      return proceedWithResponse(response, timeToLastByteMicros);
    }

    @Override
    public void onThrowable(Throwable t) {
      org.asynchttpclient.Response response = TransportExceptionMapper.map(t, request.getUri());
      long timeToLastByteMicros = getTimeToLastByte();

      mdcCopy.doInContext(
          () -> LOGGER.warn(
              "HTTP_CLIENT_ERROR: client error after {} micros on {} {}: {}{}",
              timeToLastByteMicros,
              request.getMethod(),
              request.getUri(),
              t,
              response != null ? " (mapped to " + response.getStatusCode() + "), proceeding" : ", propagating"));
      String errorDescription = "";
      if (response != null) {
        errorDescription = String.format("code:%d; description:%s", response.getStatusCode(), response.getStatusText());
      }
      span.setStatus(StatusCode.ERROR, errorDescription);
      span.end();
      LOGGER.trace("span closed: {}", span);

      if (response != null) {
        proceedWithResponse(response, timeToLastByteMicros);
        return;
      }

      requestDebugs.forEach(debug -> debug.onClientProblem(t));
      requestDebugs.forEach(RequestDebug::onProcessingFinished);

      completeExceptionally(t);
    }

    private ResponseWrapper proceedWithResponse(org.asynchttpclient.Response response, long responseTimeMicros) {
      Response debuggedResponse = new Response(response);
      for (RequestDebug debug : requestDebugs) {
        debuggedResponse = debug.onResponse(debuggedResponse);
      }
      ResponseWrapper wrapper = new ResponseWrapper(debuggedResponse, responseTimeMicros);
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
            LOGGER.error("Failed to complete promise exceptionally in a separate thread: {}, using ning thread", e, e);
          }
        });
        completeExceptionallyTask.run();
      }
    }

    private long getTimeToLastByte() {
      return requestStart.until(now(), ChronoUnit.MICROS);
    }
  }
}
