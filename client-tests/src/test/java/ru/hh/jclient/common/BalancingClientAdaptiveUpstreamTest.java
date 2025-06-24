package ru.hh.jclient.common;

import ru.hh.jclient.common.balancing.config.BalancingStrategyType;

public class BalancingClientAdaptiveUpstreamTest extends BalancingClientTestBase {
  @Override
  protected boolean isAdaptiveClient() {
    return false;
  }

  @Override
  protected BalancingStrategyType getBalancingStrategyTypeForUpstream() {
    return BalancingStrategyType.ADAPTIVE;
  }
}
