package ru.hh.jclient.common;

import java.util.function.UnaryOperator;

public class DefaultRequestStrategy implements RequestStrategy<RequestEngineBuilder> {

  public DefaultRequestStrategy() {
  }

  @Override
  public RequestEngineBuilder createRequestEngineBuilder(HttpClient client) {
    return new DefaultEngineBuilder(client);
  }

  @Override
  public void setTimeoutMultiplier(double timeoutMultiplier) {
  }

  @Override
  public RequestStrategy<RequestEngineBuilder> createCustomizedCopy(UnaryOperator<RequestEngineBuilder> configAction) {
    throw new IllegalCallerException("There is no customization for default strategy");
  }
}
