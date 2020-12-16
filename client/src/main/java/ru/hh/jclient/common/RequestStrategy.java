package ru.hh.jclient.common;

import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public interface RequestStrategy<REB extends RequestEngineBuilder<REB>> {

  interface RequestExecutor {
    CompletableFuture<ResponseWrapper> executeRequest(Request request, int retryCount, RequestContext context);
    int getDefaultRequestTimeoutMs();
  }
  REB createRequestEngineBuilder(HttpClient client);
  RequestStrategy<REB> createCustomizedCopy(UnaryOperator<REB> configAction);
}
