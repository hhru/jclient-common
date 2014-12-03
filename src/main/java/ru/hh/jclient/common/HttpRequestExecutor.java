package ru.hh.jclient.common;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import ru.hh.jclient.common.exception.ClientRequestException;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

class HttpRequestExecutor {

  private AsyncHttpClient http;
  private HttpClient context;

  HttpRequestExecutor(AsyncHttpClient http, HttpClient context) {
    this.http = http;
    this.context = context;
  }

  public <T> CompletableFuture<T> request(HttpClient context) {
    Request request = context
        .getRequestBuilder()
        .setHeaders(context.getContext().getHeaders().asMap())
        .addHeader("Content-type", context.getReturnType().getMediaType().toString())
        .build();
    return request(request).thenApply(context.getReturnType().converterFunction(context));
  }

  private CompletableFuture<Response> request(Request request) {
    CompletableFuture<Response> promise = new CompletableFuture<>();
    try {
      http.executeRequest(request, new CompletionHandler(promise));
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
