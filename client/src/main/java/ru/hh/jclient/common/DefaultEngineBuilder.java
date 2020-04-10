package ru.hh.jclient.common;

public class DefaultEngineBuilder implements RequestEngineBuilder {
  private final HttpClient httpClient;

  public DefaultEngineBuilder(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public RequestEngine build(Request request, RequestStrategy.RequestExecutor executor) {
    if (request.getRequestTimeout() <= 0) {
      return () -> {
        var requestWithTimeout = new RequestBuilder(request).setRequestTimeout(executor.getDefaultRequestTimeoutMs()).build();
        return executor.executeRequest(requestWithTimeout, 0, RequestContext.EMPTY_CONTEXT)
            .thenApply(ResponseWrapper::getResponse);
      };
    }
    return () -> executor.executeRequest(request, 0, RequestContext.EMPTY_CONTEXT)
        .thenApply(ResponseWrapper::getResponse);
  }

  @Override
  public HttpClient backToClient() {
    return httpClient;
  }
}
