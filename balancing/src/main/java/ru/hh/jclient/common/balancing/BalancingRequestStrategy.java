package ru.hh.jclient.common.balancing;

import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.RequestStrategy;

import java.util.function.UnaryOperator;

public class BalancingRequestStrategy implements RequestStrategy<RequestBalancerBuilder> {

  private final UpstreamManager upstreamManager;
  private final UnaryOperator<RequestBalancerBuilder> configAction;

  public BalancingRequestStrategy(UpstreamManager upstreamManager) {
    this(upstreamManager, UnaryOperator.identity());
  }

  private BalancingRequestStrategy(UpstreamManager upstreamManager, UnaryOperator<RequestBalancerBuilder> configAction) {
    this.upstreamManager = upstreamManager;
    this.configAction = configAction;
  }

  @Override
  public RequestBalancerBuilder createRequestEngineBuilder(HttpClient client) {
    var builder = new RequestBalancerBuilder(upstreamManager, client);
    return configAction.apply(builder);
  }

  @Override
  public BalancingRequestStrategy createCustomizedCopy(UnaryOperator<RequestBalancerBuilder> configAction) {
    return new BalancingRequestStrategy(this.upstreamManager, configAction);
  }
}
