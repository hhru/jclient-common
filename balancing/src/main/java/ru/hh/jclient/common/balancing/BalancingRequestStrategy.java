package ru.hh.jclient.common.balancing;

import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.RequestEngineBuilder;
import ru.hh.jclient.common.RequestStrategy;

public class BalancingRequestStrategy implements RequestStrategy<RequestBalancer> {

  private final UpstreamManager upstreamManager;

  public BalancingRequestStrategy(UpstreamManager upstreamManager) {
    this.upstreamManager = upstreamManager;
  }

  @Override
  public RequestEngineBuilder<RequestBalancer> getRequestEngineBuilder(HttpClient client) {
    return new RequestBalancerBuilder(upstreamManager, client);
  }

  public UpstreamManager getUpstreamManager() {
    return upstreamManager;
  }

  @Override
  public void setTimeoutMultiplier(double timeoutMultiplier) {
    upstreamManager.setTimeoutMultiplier(timeoutMultiplier);
  }
}
