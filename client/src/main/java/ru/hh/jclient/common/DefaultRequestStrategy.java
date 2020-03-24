package ru.hh.jclient.common;

import java.util.function.UnaryOperator;

public class DefaultRequestStrategy implements RequestStrategy<RequestEngine, RequestEngineBuilder<RequestEngine>> {

  public DefaultRequestStrategy() {
  }

  @Override
  public RequestEngineBuilder<RequestEngine> createRequestEngineBuilder(HttpClient client) {
    return new DefaultEngineBuilder(client);
  }

  @Override
  public void setTimeoutMultiplier(double timeoutMultiplier) {
  }

  @Override
  public RequestStrategy<RequestEngine, RequestEngineBuilder<RequestEngine>> createCustomizedCopy(
      UnaryOperator<RequestEngineBuilder<RequestEngine>> configAction
  ) {
    throw new IllegalCallerException("There is no customization for default strategy");
  }
}
