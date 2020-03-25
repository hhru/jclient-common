package ru.hh.jclient.common;

import java.util.function.UnaryOperator;

public class HttpClientFactoryConfigurator {
  protected UnaryOperator<? extends RequestEngineBuilder> requestStrategyConfigurator() {
    return UnaryOperator.identity();
  }

  public HttpClientFactory configure(HttpClientFactory factory) {
    return factory.createCustomizedCopy(requestStrategyConfigurator());
  }
}
