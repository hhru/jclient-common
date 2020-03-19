package ru.hh.jclient.common;

import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public interface RequestStrategy<RE extends RequestEngine, REB extends RequestEngineBuilder<RE>> {

  @FunctionalInterface
  interface RequestExecutor {
    CompletableFuture<ResponseWrapper> executeRequest(Request request, int retryCount, RequestContext context);
  }
  REB createRequestEngineBuilder(HttpClient client);
  void setTimeoutMultiplier(double timeoutMultiplier);
  RequestStrategy<RE, REB> customized(UnaryOperator<REB> configAction);
}
