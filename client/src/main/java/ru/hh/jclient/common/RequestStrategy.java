package ru.hh.jclient.common;

import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public interface RequestStrategy<REB extends RequestEngineBuilder> {

  interface RequestExecutor {
    CompletableFuture<ResponseWrapper> executeRequest(Request request, int retryCount, RequestContext context);
    int getDefaultRequestTimeoutMs();
  }
  REB createRequestEngineBuilder(HttpClient client);
  void setTimeoutMultiplier(double timeoutMultiplier);
  RequestStrategy<REB> createCustomizedCopy(UnaryOperator<REB> configAction);
}
