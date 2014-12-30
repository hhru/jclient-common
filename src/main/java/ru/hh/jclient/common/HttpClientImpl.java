package ru.hh.jclient.common;

import static com.google.common.collect.ImmutableSet.of;
import static java.lang.Boolean.TRUE;
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

  private static final Set<String> PASS_THROUGH_HEADERS = of(HEADER_REQUEST_ID, HEADER_REAL_IP, HEADER_AUTH, HEADER_SESSION, HEADER_DEBUG);

  HttpClientImpl(AsyncHttpClient http, Request request, Set<String> hostsWithSession, Supplier<HttpRequestContext> contextSupplier,
      Supplier<HttpRequestInfo> infoSupplier) {
    super(http, request, hostsWithSession, contextSupplier, infoSupplier);
  }

  <T> CompletableFuture<T> executeRequest() {
    RequestBuilder builder = new RequestBuilder(getRequest());
    addHeaders(builder);
    if (useReadOnlyReplica()) {
      builder.addQueryParam(PARAM_READ_ONLY_REPLICA, TRUE.toString());
    }

    Request request = builder.build();
    CompletableFuture<T> future = request(request).thenApply(getInfo()::onResponseReceived).thenApply(getReturnType().converterFunction(this));
    return future.thenApply(getInfo()::onProcessingFinished);
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
      headers.remove(HEADER_SESSION);
    }

    // remove debug/auth headers if debug is not enabled
    if (!getContext().isDebugMode()) {
      headers.remove(HEADER_DEBUG);
      headers.remove(HEADER_AUTH);
    }

    requestBuilder.setHeaders(headers);
  }

  private CompletableFuture<Response> request(Request request) {
    CompletableFuture<Response> promise = new CompletableFuture<>();
    getInfo().onRequest(request);
    getHttp().executeRequest(request, new CompletionHandler(promise));
    return promise;
  }

  private static class CompletionHandler extends AsyncCompletionHandler<Response> {

    private CompletableFuture<Response> promise;

    public CompletionHandler(CompletableFuture<Response> promise) {
      this.promise = promise;
    }

    @Override
    public Response onCompleted(Response response) throws Exception {
      // TODO add proper processing of >=400 status codes
      if (response.getStatusCode() >= 400) {
        throw new ClientResponseException(response);
      }
      promise.complete(response);
      return response;
    }

    @Override
    public void onThrowable(Throwable t) {
      promise.completeExceptionally(t);
      super.onThrowable(t);
    }
  }
}
