package ru.hh.jclient.common;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import ru.hh.jclient.common.exception.ClientRequestException;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;

class HttpClientImpl extends HttpClient {

  HttpClientImpl(AsyncHttpClient http, Supplier<HttpRequestContext> contextSupplier, Set<String> hostsWithSession, RequestBuilder requestBuilder) {
    super(http, contextSupplier, hostsWithSession, requestBuilder);
  }

  <T> CompletableFuture<T> executeRequest() {
    Request request = getRequestBuilder()
        .setHeaders(getContext().getHeaders().asMap())
        .addHeader("Content-type", getReturnType().getMediaType().toString())
        .build();
    return request(request).thenApply(getReturnType().converterFunction(this));
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
