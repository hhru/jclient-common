package ru.hh.jclient.common;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static java.lang.Boolean.TRUE;
import static ru.hh.jclient.common.HttpHeaders.X_HH_DEBUG;
import static ru.hh.jclient.common.HttpHeaders.X_REAL_IP;
import static ru.hh.jclient.common.HttpHeaders.X_REQUEST_ID;
import static ru.hh.jclient.common.HttpHeaders.HH_PROTO_SESSION;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import ru.hh.jclient.common.exception.ClientResponseException;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;

class HttpClientImpl extends HttpClient {

  private static final String PARAM_READ_ONLY_REPLICA = "replicaOnlyRq";

  private static final Set<String> PASS_THROUGH_HEADERS = of(X_REQUEST_ID, X_REAL_IP, AUTHORIZATION, HH_PROTO_SESSION, X_HH_DEBUG);

  HttpClientImpl(AsyncHttpClient http, Request request, Set<String> hostsWithSession, Supplier<HttpClientContext> contextSupplier) {
    super(http, request, hostsWithSession, contextSupplier);
  }

  <T> CompletableFuture<T> executeRequest() {
    RequestBuilder builder = new RequestBuilder(getRequest());
    addHeaders(builder);
    if (useReadOnlyReplica()) {
      builder.addQueryParam(PARAM_READ_ONLY_REPLICA, TRUE.toString());
    }

    Request request = builder.build();
    CompletableFuture<T> future = request(request).thenApply(r -> {
      try {
        return getReturnType().<T> converterFunction(this).apply(r);
      }
      catch (RuntimeException e) {
        getDebug().onConvertorProblem(e);
        throw e;
      }
      finally {
        getDebug().onProcessingFinished();
      }
    });
    return future;
  }

  private void addHeaders(RequestBuilder requestBuilder) {
    // compute headers. Headers from context are used as base, with headers from request overriding any existing values
    FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();
    PASS_THROUGH_HEADERS
        .stream()
        .map(String::toLowerCase)
        .filter(getContext().getHeaders()::containsKey)
        .forEach(h -> headers.add(h, getContext().getHeaders().get(h)));

    headers.putAll(getRequest().getHeaders());

    // remove hh-session header if host does not need it
    if (getHostsWithSession().stream().noneMatch(h -> getRequest().getUrl().startsWith(h))) {
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
    getHttp().executeRequest(request, new CompletionHandler(promise, getDebug()));
    return promise;
  }

  private static class CompletionHandler extends AsyncCompletionHandler<Response> {

    private CompletableFuture<Response> promise;
    private RequestDebug requestDebug;

    public CompletionHandler(CompletableFuture<Response> promise, RequestDebug requestDebug) {
      this.promise = promise;
      this.requestDebug = requestDebug;
    }

    @Override
    public Response onCompleted(Response response) throws Exception {
      requestDebug.onResponse(response);
      // TODO add proper processing of >=400 status codes
      if (response.getStatusCode() >= 400) {
        requestDebug.onProcessingFinished();
        promise.completeExceptionally(new ClientResponseException(response));
      }
      promise.complete(response);
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
