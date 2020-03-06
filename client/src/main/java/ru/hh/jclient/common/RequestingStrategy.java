package ru.hh.jclient.common;

import java.util.concurrent.CompletableFuture;

public interface RequestingStrategy<T extends RequestEngine> {

  @FunctionalInterface
  interface RequestExecutor {
    CompletableFuture<ResponseWrapper> executeRequest(Request request, int retryCount, RequestContext context);
  }
  UpstreamManager getUpstreamManager();
  RequestEngineBuilder<T> getRequestEngineBuilder(HttpClient client);
}
