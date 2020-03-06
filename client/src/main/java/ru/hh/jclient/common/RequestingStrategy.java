package ru.hh.jclient.common;

import java.util.concurrent.CompletableFuture;

public interface RequestingStrategy<T extends RequestEngine> {

  @FunctionalInterface
  interface RequestExecutor {
    CompletableFuture<ResponseWrapper> executeRequest(Request request, int retryCount, RequestContext context);
  }
  RequestEngineBuilder<T> getRequestEngineBuilder(HttpClient client);
  void setTimeoutMultiplier(double timeoutMultiplier);
}
