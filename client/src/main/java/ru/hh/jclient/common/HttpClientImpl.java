package ru.hh.jclient.common;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Param;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.uri.Uri;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.util.MDCCopy;
import static com.google.common.collect.ImmutableSet.of;
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

  HttpClientImpl(AsyncHttpClient http, Request request, Set<String> hostsWithSession, Supplier<HttpClientContext> contextSupplier) {
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
    getHttp().executeRequest(request, new CompletionHandler(promise, request, now(), getDebug(), getHttp().getConfig()));
    return promise;
  }

  private void addHeadersAndParams(RequestBuilder requestBuilder) {
    // add readonly param
    if (useReadOnlyReplica()) {
      requestBuilder.addQueryParam(READ_ONLY_REPLICA, TRUE.toString());
    }

    // add debug param
    if (getContext().isDebugMode()) {
      requestBuilder.addQueryParam(HttpParams.DEBUG, HttpParams.getDebugValue());
    }

    // compute headers. Headers from context are used as base, with headers from request overriding any existing values
    FluentCaseInsensitiveStringsMap headers = isExternal() ? new FluentCaseInsensitiveStringsMap() : PASS_THROUGH_HEADERS
        .stream()
        .filter(getContext().getHeaders()::containsKey)
        .collect(toFluentCaseInsensitiveStringsMap(identity(), h -> getContext().getHeaders().get(h)));

    headers.addAll(getRequest().getHeaders());

    // remove hh-session header if host does not need it
    if (isNoSession() || getHostsWithSession().stream().map(Uri::create).map(Uri::getHost).noneMatch(h -> getRequest().getUri().getHost().equals(h))) {
      headers.remove(HH_PROTO_SESSION);
    }

    // remove debug/auth headers if debug is not enabled
    if (isNoDebug() || !getContext().isDebugMode()) {
      headers.remove(X_HH_DEBUG);
      headers.remove(AUTHORIZATION);
      List<Param> params = getRequest()
          .getQueryParams()
          .stream()
          .filter(param -> !param.getName().equals(HttpParams.DEBUG))
          .collect(Collectors.toList());
      requestBuilder.setQueryParams(params);
    }

    requestBuilder.setHeaders(headers);
  }

  static class CompletionHandler extends AsyncCompletionHandler<Response> {

    private MDCCopy mdcCopy;
    private CompletableFuture<Response> promise;
    private Request request;
    private Instant requestStart;
    private RequestDebug requestDebug;
    private AsyncHttpClientConfig config;

    public CompletionHandler(CompletableFuture<Response> promise, Request request, Instant requestStart, RequestDebug requestDebug,
                             AsyncHttpClientConfig config) {
      this.requestStart = requestStart;
      this.mdcCopy = MDCCopy.capture();
      this.promise = promise;
      this.request = request;
      this.requestDebug = requestDebug;
      this.config = config;
    }

    @Override
    public Response onCompleted(Response response) throws Exception {
      int responseStatusCode = response.getStatusCode();
      String responseStatusText = response.getStatusText();
      response = requestDebug.onResponse(config, response);
      mdcCopy.doInContext( () -> log.info("ASYNC_HTTP_RESPONSE: {} {} in {} ms on {} {}", responseStatusCode, responseStatusText,
        requestStart.until(now(), ChronoUnit.MILLIS), request.getMethod(), request.getUri()));
      promise.complete(response);
      // TODO requestDebug.onProcessingFinished(); maybe should be here?
      return response;
    }

    @Override
    public void onThrowable(Throwable t) {
      requestDebug.onClientProblem(t);
      requestDebug.onProcessingFinished();
      mdcCopy.doInContext(() -> log.warn("ASYNC_HTTP_ERROR: client error after {} ms on {} {}", requestStart.until(now(), ChronoUnit.MILLIS),
        request.getMethod(), request.getUri()));
      promise.completeExceptionally(t);
    }
  }
}
