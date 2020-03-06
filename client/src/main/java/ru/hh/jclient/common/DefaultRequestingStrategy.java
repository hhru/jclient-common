package ru.hh.jclient.common;

public class DefaultRequestingStrategy implements RequestingStrategy<RequestEngine> {
  private final DefaultUpstreamManager upstreamManager;

  public DefaultRequestingStrategy(DefaultUpstreamManager upstreamManager) {
    this.upstreamManager = upstreamManager;
  }

  @Override
  public UpstreamManager getUpstreamManager() {
    return upstreamManager;
  }

  @Override
  public RequestEngineBuilder<RequestEngine> getRequestEngineBuilder(HttpClient client) {
    return new DefaultEngineBuilder(client);
  }
}
