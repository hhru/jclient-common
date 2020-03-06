package ru.hh.jclient.common;

public class DefaultRequestingStrategy implements RequestingStrategy<RequestEngine> {

  public DefaultRequestingStrategy() {
  }

  @Override
  public RequestEngineBuilder<RequestEngine> getRequestEngineBuilder(HttpClient client) {
    return new DefaultEngineBuilder(client);
  }

  @Override
  public void setTimeoutMultiplier(double timeoutMultiplier) {
  }
}
