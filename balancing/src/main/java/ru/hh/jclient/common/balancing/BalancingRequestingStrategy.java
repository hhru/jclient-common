package ru.hh.jclient.common.balancing;

import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.RequestEngineBuilder;
import ru.hh.jclient.common.RequestingStrategy;

public class BalancingRequestingStrategy implements RequestingStrategy<RequestBalancer> {

  private final BalancingUpstreamManager upstreamManager;

  public BalancingRequestingStrategy(BalancingUpstreamManager upstreamManager) {
    this.upstreamManager = upstreamManager;
  }

  @Override
  public RequestEngineBuilder<RequestBalancer> getRequestEngineBuilder(HttpClient client) {
    return new RequestBalancerBuilder(upstreamManager, client);
  }

  public BalancingUpstreamManager getUpstreamManager() {
    return upstreamManager;
  }

  @Override
  public void setTimeoutMultiplier(double timeoutMultiplier) {
    upstreamManager.setTimeoutMultiplier(timeoutMultiplier);
  }
}
