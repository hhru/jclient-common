package ru.hh.jclient.common;

public class DefaultRequestStrategy implements RequestStrategy<RequestEngine> {

  public DefaultRequestStrategy() {
  }

  @Override
  public RequestEngineBuilder<RequestEngine> getRequestEngineBuilder(HttpClient client) {
    return new DefaultEngineBuilder(client);
  }

  @Override
  public void setTimeoutMultiplier(double timeoutMultiplier) {
  }
}
