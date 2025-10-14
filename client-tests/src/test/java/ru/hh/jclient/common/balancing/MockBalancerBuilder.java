package ru.hh.jclient.common.balancing;

import java.util.concurrent.CompletableFuture;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestContext;
import ru.hh.jclient.common.RequestResponseWrapper;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.Response;

public final class MockBalancerBuilder extends RequestBalancerBuilder {

  private final Response mock;

  public MockBalancerBuilder(UpstreamManager upstreamManager, HttpClient client, Response responseMock) {
    super(upstreamManager, client);
    this.mock = responseMock;
    this.balancingRequestsLogLevel = "ERROR";
  }

  @Override
  public RequestBalancer build(Request request, RequestStrategy.RequestExecutor requestExecutor) {
    return super.build(request, new RequestStrategy.RequestExecutor() {
      @Override
      public CompletableFuture<RequestResponseWrapper> executeRequest(Request request, int retryCount, RequestContext context) {
        return CompletableFuture.completedFuture(new RequestResponseWrapper(request, mock, 1));
      }

      @Override
      public CompletableFuture<RequestResponseWrapper> handleFailFastResponse(Request request, RequestContext requestContext, Response response) {
        return CompletableFuture.completedFuture(new RequestResponseWrapper(request, response, 0L));
      }

      @Override
      public int getDefaultRequestTimeoutMs() {
        return 0;
      }
    });
  }
}
