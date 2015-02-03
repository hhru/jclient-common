package ru.hh.jclient.common;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static java.lang.Boolean.TRUE;
import static java.util.function.Function.identity;
import static ru.hh.jclient.common.HttpHeaders.X_HH_DEBUG;
import static ru.hh.jclient.common.HttpHeaders.X_REAL_IP;
import static ru.hh.jclient.common.HttpHeaders.X_REQUEST_ID;
import static ru.hh.jclient.common.HttpHeaders.HH_PROTO_SESSION;
import static ru.hh.jclient.common.util.MoreCollectors.toFluentCaseInsensitiveStringsMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import ru.hh.jclient.common.exception.ClientResponseException;
import ru.hh.jclient.common.exception.ResponseConverterException;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.uri.Uri;

class HttpClientImpl extends HttpClient {

  static final String PARAM_READ_ONLY_REPLICA = "replicaOnlyRq";

  static final Set<String> PASS_THROUGH_HEADERS = of(X_REQUEST_ID, X_REAL_IP, AUTHORIZATION, HH_PROTO_SESSION, X_HH_DEBUG);

  HttpClientImpl(AsyncHttpClient http, Request request, Set<String> hostsWithSession, Supplier<HttpClientContext> contextSupplier) {
    super(http, request, hostsWithSession, contextSupplier);
  }

  <T> CompletableFuture<ResponseWrapper<T>> executeRequest(FailableFunction<Response, ResponseWrapper<T>, Exception> converter) {
    RequestBuilder builder = new RequestBuilder(getRequest());
    addHeaders(builder);
    if (useReadOnlyReplica()) {
      builder.addQueryParam(PARAM_READ_ONLY_REPLICA, TRUE.toString());
    }

    Request request = builder.build();
    CompletableFuture<ResponseWrapper<T>> future = request(request).thenApply(r -> {
      try {
        return converter.apply(r);
      }
      catch (Exception e) {
        ResponseConverterException rce = new ResponseConverterException(e);
        getDebug().onConverterProblem(rce);
        throw rce;
      }
      finally {
        getDebug().onProcessingFinished();
      }
    });
    return future;
  }

  private void addHeaders(RequestBuilder requestBuilder) {
    // compute headers. Headers from context are used as base, with headers from request overriding any existing values
    FluentCaseInsensitiveStringsMap headers = PASS_THROUGH_HEADERS
        .stream()
        .filter(getContext().getHeaders()::containsKey)
        .collect(toFluentCaseInsensitiveStringsMap(identity(), h -> getContext().getHeaders().get(h)));

    headers.addAll(getRequest().getHeaders());

    // remove hh-session header if host does not need it
    if (getHostsWithSession().stream().map(Uri::create).map(Uri::getHost).noneMatch(h -> getRequest().getUri().getHost().equals(h))) {
      headers.remove(HH_PROTO_SESSION);
    }

    // remove debug/auth headers if debug is not enabled
    if (!getContext().isDebugMode()) {
      headers.remove(X_HH_DEBUG);
      headers.remove(AUTHORIZATION);
    }

    requestBuilder.setHeaders(headers);
  }

  private CompletableFuture<Response> request(Request request) {
    CompletableFuture<Response> promise = new CompletableFuture<>();
    getDebug().onRequest(getHttp().getConfig(), request);
    getHttp().executeRequest(request, new CompletionHandler(promise, getDebug(), getHttp().getConfig()));
    return promise;
  }

  static class CompletionHandler extends AsyncCompletionHandler<Response> {

    private CompletableFuture<Response> promise;
    private RequestDebug requestDebug;
    private AsyncHttpClientConfig config;

    public CompletionHandler(CompletableFuture<Response> promise, RequestDebug requestDebug, AsyncHttpClientConfig config) {
      this.promise = promise;
      this.requestDebug = requestDebug;
      this.config = config;
    }

    @Override
    public Response onCompleted(Response response) throws Exception {
      response = requestDebug.onResponse(config, response);
      // TODO add proper processing of >=400 status codes
      if (response.getStatusCode() >= 400) {
        requestDebug.onProcessingFinished();
        promise.completeExceptionally(new ClientResponseException(response));
        // do not report to debug because it is only about bad response status
      }
      else {
        promise.complete(response);
      }
      return response;
    }

    @Override
    public void onThrowable(Throwable t) {
      requestDebug.onClientProblem(t);
      requestDebug.onProcessingFinished();
      promise.completeExceptionally(t);
    }
  }
}
