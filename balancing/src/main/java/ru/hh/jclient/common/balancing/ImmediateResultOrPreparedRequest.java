package ru.hh.jclient.common.balancing;

import jakarta.annotation.Nonnull;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.RequestContext;
import ru.hh.jclient.common.Response;

public final class ImmediateResultOrPreparedRequest {
  private final Response result;
  private final RequestContext requestContext;
  private final Request processedRequest;

  public ImmediateResultOrPreparedRequest(@Nonnull Response result, @Nonnull RequestContext requestContext) {
    this.result = result;
    this.requestContext = requestContext;
    this.processedRequest = null;
  }

  public ImmediateResultOrPreparedRequest(@Nonnull RequestContext requestContext, @Nonnull Request processedRequest) {
    this.result = null;
    this.requestContext = requestContext;
    this.processedRequest = processedRequest;
  }

  public Response getResult() {
    return result;
  }

  public RequestContext getRequestContext() {
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
