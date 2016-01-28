package ru.hh.jclient.common;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.uri.Uri;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.util.MDCCopy;
import ru.hh.jclient.common.util.storage.StorageUtils.Transfers;
import ru.hh.jclient.common.util.storage.Storage;
import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static java.lang.Boolean.TRUE;
import static java.time.Instant.now;
import static java.util.function.Function.identity;
import static ru.hh.jclient.common.HttpHeaders.HH_PROTO_SESSION;
import static ru.hh.jclient.common.HttpHeaders.X_HH_DEBUG;
import static ru.hh.jclient.common.HttpHeaders.X_REAL_IP;
import static ru.hh.jclient.common.HttpHeaders.X_REQUEST_ID;
import static ru.hh.jclient.common.HttpParams.READ_ONLY_REPLICA;
import static ru.hh.jclient.common.util.MoreCollectors.toFluentCaseInsensitiveStringsMap;

class HttpClientImpl extends HttpClient {

  static final Set<String> PASS_THROUGH_HEADERS = of(X_REQUEST_ID, X_REAL_IP, AUTHORIZATION, HH_PROTO_SESSION, X_HH_DEBUG);

  private static final Logger log = LoggerFactory.getLogger(HttpClientImpl.class);

  HttpClientImpl(AsyncHttpClient http, Request request, Set<String> hostsWithSession, Storage<HttpClientContext> contextSupplier) {
    super(http, request, hostsWithSession, contextSupplier);
  }

  @Override
  CompletableFuture<Response> executeRequest() {
    RequestBuilder builder = new RequestBuilder(getRequest());
    addHeadersAndParams(builder);

    Request request = builder.build();
    CompletableFuture<Response> promise = new CompletableFuture<>();
    getDebug().onRequest(getHttp().getConfig(), request, requestBodyEntity);
    log.debug("ASYNC_HTTP_START: Starting {} {}", request.getMethod(), request.getUri());
    Transfers transfers = getStorages().prepare();
    getHttp().executeRequest(request, new CompletionHandler(promise, request, now(), getDebug(), transfers, getHttp().getConfig()));
    return promise;
  }

  private void addHeadersAndParams(RequestBuilder requestBuilder) {
    // compute headers. Headers from context are used as base, with headers from request overriding any existing values
    FluentCaseInsensitiveStringsMap headers = isExternal() ? new FluentCaseInsensitiveStringsMap() : PASS_THROUGH_HEADERS
        .stream()
        .filter(getContext().getHeaders()::containsKey)
        .collect(toFluentCaseInsensitiveStringsMap(identity(), h -> getContext().getHeaders().get(h)));

    // debug header is passed through by default, but should be removed before final check at the end if client specifies so
    if (isNoDebug() || isExternal() || !getContext().isDebugMode()) {
      headers.remove(X_HH_DEBUG);
    }

    headers.addAll(getRequest().getHeaders());

    // remove hh-session header if host does not need it
    if (isNoSession() || getHostsWithSession().stream().map(Uri::create).map(Uri::getHost).noneMatch(h -> getRequest().getUri().getHost().equals(h))) {
      headers.remove(HH_PROTO_SESSION);
    }

    requestBuilder.setHeaders(headers);

    if (!headers.containsKey(ACCEPT) && getExpectedMediaTypes().isPresent()) {
      requestBuilder.addHeader(ACCEPT, getExpectedMediaTypes().get().stream().map(Object::toString).collect(Collectors.joining(",")));
    }

    // add readonly param
    if (useReadOnlyReplica() && !isExternal()) {
      requestBuilder.addQueryParam(READ_ONLY_REPLICA, TRUE.toString());
    }

    // add both debug param and debug header (for backward compatibility)
    if (getContext().isDebugMode() && !isNoDebug() && !isExternal()) {
      requestBuilder.setHeader(X_HH_DEBUG, "true");
      requestBuilder.addQueryParam(HttpParams.DEBUG, HttpParams.getDebugValue());
    }

    // sanity check for debug header/param if debug is not enabled
    if (isNoDebug() || isExternal() || !getContext().isDebugMode()) {
      if (headers.containsKey(X_HH_DEBUG)) {
        throw new IllegalStateException("Debug header in request when debug is disabled");
      }

      if (getRequest().getQueryParams().stream().anyMatch(param -> param.getName().equals(HttpParams.DEBUG))) {
        throw new IllegalStateException("Debug param in request when debug is disabled");
      }
    }
  }

  static class CompletionHandler extends AsyncCompletionHandler<Response> {

    private MDCCopy mdcCopy;
    private CompletableFuture<Response> promise;
    private Request request;
    private Instant requestStart;
    private RequestDebug requestDebug;
    private AsyncHttpClientConfig config;
    private Transfers contextTransfers;

    public CompletionHandler(
        CompletableFuture<Response> promise,
        Request request,
        Instant requestStart,
        RequestDebug requestDebug,
        Transfers contextTransfers,
        AsyncHttpClientConfig config) {
      this.requestStart = requestStart;
      this.mdcCopy = MDCCopy.capture();
      this.promise = promise;
      this.request = request;
      this.requestDebug = requestDebug;
      this.contextTransfers = contextTransfers;
      this.config = config;
    }

    @Override
    public Response onCompleted(Response response) throws Exception {
      int responseStatusCode = response.getStatusCode();
      String responseStatusText = response.getStatusText();
      response = requestDebug.onResponse(config, response);
      mdcCopy.doInContext( () -> log.info("ASYNC_HTTP_RESPONSE: {} {} in {} ms on {} {}", responseStatusCode, responseStatusText,
        requestStart.until(now(), ChronoUnit.MILLIS), request.getMethod(), request.getUri()));

      // calling promise.complete() will block until anything that was chained to promise completes
      // install context(s) for current (ning) thread so chained tasks have context to run with
      // remove context(s) once the promise completes
      try {
        contextTransfers.perform();
        promise.complete(response);
      }
      finally {
        contextTransfers.rollback();
      }
      return response;
    }

    @Override
    public void onThrowable(Throwable t) {
      requestDebug.onClientProblem(t);
      requestDebug.onProcessingFinished();
      mdcCopy.doInContext(
          () -> log.warn(
              "ASYNC_HTTP_ERROR: client error after {} ms on {} {}: {}",
              requestStart.until(now(), ChronoUnit.MILLIS),
              request.getMethod(),
              request.getUri(),
              t.getMessage()));

      // calling promise.completeExceptionally() will block until anything that was chained to promise completes
      // install context(s) for current (ning) thread so chained tasks have context to run with
      // remove context(s) once the promise completes
      try {
        contextTransfers.perform();
        promise.completeExceptionally(t);
      }
      finally {
        contextTransfers.rollback();
      }
    }
  }
}
