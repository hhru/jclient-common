package ru.hh.jclient.common.balancing;

import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.RequestContext;
import ru.hh.jclient.common.Response;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public final class ImmediateResultOrPreparedRequest {
  private final CompletableFuture<Response> result;
  private final RequestContext requestContext;
  private final Request processedRequest;

  public ImmediateResultOrPreparedRequest(@Nonnull CompletableFuture<Response> result) {
    this.result = result;
    this.requestContext = null;
    this.processedRequest = null;
  }

  public ImmediateResultOrPreparedRequest(@Nonnull RequestContext requestContext, @Nonnull Request processedRequest) {
    this.result = null;
    this.requestContext = requestContext;
    this.processedRequest = processedRequest;
  }

  public CompletableFuture<Response> getResult() {
    return result;
  }

  public RequestContext getRequestContext() {
    if (result != null) {
      throw new IllegalStateException("first check result");
    }
    return requestContext;
  }

  public Request getBalancedRequest(double timeoutMultiplier) {
    if (result != null) {
      throw new IllegalStateException("first check result");
    }
    RequestBuilder requestBuilder = new RequestBuilder(processedRequest);
    requestBuilder.setRequestTimeout((int) (processedRequest.getRequestTimeout() * timeoutMultiplier));
    return requestBuilder.build();
  }
}
