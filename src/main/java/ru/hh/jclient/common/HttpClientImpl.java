package ru.hh.jclient.common;

import static com.google.common.collect.ImmutableSet.of;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import ru.hh.jclient.common.exception.ClientRequestException;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;

class HttpClientImpl extends HttpClient {

  private static final String HEADER_SESSION = "Hh-Proto-Session";
  private static final String HEADER_AUTH = "Authorization";
  private static final Set<String> PASS_THROUGH_HEADERS = of("X-Request-Id", "X-Real-IP", HEADER_AUTH, HEADER_SESSION, HEADER_DEBUG);

  HttpClientImpl(AsyncHttpClient http, Supplier<HttpRequestContext> contextSupplier, Set<String> hostsWithSession, Request request) {
    super(http, contextSupplier, hostsWithSession, request);
  }

  <T> CompletableFuture<T> executeRequest() {
    RequestBuilder builder = new RequestBuilder(getRequest());
    addHeaders(builder);

    Request request = builder.build();
    return request(request).thenApply(getReturnType().converterFunction(this));
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
    try {
      getHttp().executeRequest(request, new CompletionHandler(promise));
    }
    catch (IOException e) {
      promise.completeExceptionally(e);
    }
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
        throw new ClientRequestException(response);
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
