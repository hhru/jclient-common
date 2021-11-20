package ru.hh.jclient.common.balancing;

import java.util.function.UnaryOperator;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.consul.UpstreamConfigService;
import ru.hh.jclient.consul.UpstreamService;

public class BalancingRequestStrategy implements RequestStrategy<RequestBalancerBuilder> {

  private final UpstreamManager upstreamManager;
  private final UpstreamConfigService upstreamConfigService;
  private final UpstreamService upstreamService;
  private final UnaryOperator<RequestBalancerBuilder> configAction;

  public BalancingRequestStrategy(UpstreamManager upstreamManager,
                                  UpstreamService upstreamService, UpstreamConfigService upstreamConfigService) {
    this(upstreamManager, upstreamService, upstreamConfigService, UnaryOperator.identity());
  }

  @Deprecated(forRemoval = true)
  public BalancingRequestStrategy(UpstreamManager upstreamManager) {
    this(upstreamManager, null, null, UnaryOperator.identity());
  }

  private BalancingRequestStrategy(UpstreamManager upstreamManager,
                                   UpstreamService upstreamService, UpstreamConfigService upstreamConfigService,
                                   UnaryOperator<RequestBalancerBuilder> configAction) {
    this.upstreamManager = upstreamManager;
    this.upstreamConfigService = upstreamConfigService;
    this.upstreamService = upstreamService;
    this.configAction = configAction;
  }

  @Override
  public RequestBalancerBuilder createRequestEngineBuilder(HttpClient client) {
    var builder = new RequestBalancerBuilder(upstreamManager, client);
    return configAction.apply(builder);
  }

  @Override
  public BalancingRequestStrategy createCustomizedCopy(UnaryOperator<RequestBalancerBuilder> configAction) {
    UnaryOperator<RequestBalancerBuilder> chainedConfigAction = this.configAction.andThen(configAction)::apply;
    return new BalancingRequestStrategy(upstreamManager, upstreamService, upstreamConfigService, chainedConfigAction);
  }
}
