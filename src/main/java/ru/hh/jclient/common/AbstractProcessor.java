package ru.hh.jclient.common;

import java.util.concurrent.CompletableFuture;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.ning.http.client.Response;

public abstract class AbstractProcessor<T> {

  protected HttpClient httpClient;

  public AbstractProcessor(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  protected abstract FailableFunction<Response, ResponseWrapper<T>, Exception> converterFunction();

  public CompletableFuture<T> request() {
    return wrappedRequest().thenApply(ResponseWrapper::get);
  }

  public CompletableFuture<ResponseWrapper<T>> wrappedRequest() {
    return httpClient.executeRequest(converterFunction());
  }

}
