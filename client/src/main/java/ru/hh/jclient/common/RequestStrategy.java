package ru.hh.jclient.common;

import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public interface RequestStrategy<REB extends RequestEngineBuilder<REB>> {

  REB createRequestEngineBuilder(HttpClient client);
  RequestStrategy<REB> createCustomizedCopy(UnaryOperator<REB> configAction);

  interface RequestExecutor {
    CompletableFuture<ResponseWrapper> executeRequest(Request request, int retryCount, RequestContext context);
    CompletableFuture<ResponseWrapper> handleFailFastResponse(Request request, RequestContext requestContext, Response response);
    int getDefaultRequestTimeoutMs();
  }
}
