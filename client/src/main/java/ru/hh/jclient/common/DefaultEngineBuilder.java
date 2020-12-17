package ru.hh.jclient.common;

import static java.util.Optional.ofNullable;

public class DefaultEngineBuilder implements RequestEngineBuilder<DefaultEngineBuilder> {
  private final HttpClient httpClient;
  private Double timeoutMultiplier;

  public DefaultEngineBuilder(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public RequestEngine build(Request request, RequestStrategy.RequestExecutor executor) {
    return () -> {
      var requestTimeout = request.getRequestTimeout() > 0 ? request.getRequestTimeout() : executor.getDefaultRequestTimeoutMs();
      var requestWithTimeout = new RequestBuilder(request)
        .setRequestTimeout(ofNullable(timeoutMultiplier).map(multiplier -> (int) (multiplier * requestTimeout)).orElse(requestTimeout))
        .build();
      return executor.executeRequest(requestWithTimeout, 0, RequestContext.EMPTY_CONTEXT)
          .thenApply(ResponseWrapper::getResponse);
    };
  }

  @Override
  public DefaultEngineBuilder withTimeoutMultiplier(Double timeoutMultiplier) {
    this.timeoutMultiplier = timeoutMultiplier;
    return this;
  }

  @Override
  public HttpClient backToClient() {
    return httpClient;
  }
}
