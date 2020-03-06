package ru.hh.jclient.common;

public class DefaultEngineBuilder implements RequestEngineBuilder<RequestEngine> {
  private final HttpClient httpClient;

  public DefaultEngineBuilder(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public RequestEngine build(Request request, RequestingStrategy.RequestExecutor executor) {
    return () -> executor.executeRequest(request, 0, RequestContext.EMPTY_CONTEXT)
        .thenApply(ResponseWrapper::getResponse);
  }

  @Override
  public HttpClient backToClient() {
    return httpClient;
  }
}
