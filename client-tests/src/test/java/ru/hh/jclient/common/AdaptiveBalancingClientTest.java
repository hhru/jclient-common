package ru.hh.jclient.common;

public class AdaptiveBalancingClientTest extends BalancingClientTestBase {

  @Override
  protected boolean isAdaptive() {
    return true;
  }
}
