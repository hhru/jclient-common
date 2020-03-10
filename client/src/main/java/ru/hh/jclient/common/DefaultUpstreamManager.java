package ru.hh.jclient.common;

import ru.hh.jclient.common.balancing.Upstream;
import ru.hh.jclient.common.balancing.UpstreamProfileSelector;

import java.util.Set;

final class DefaultUpstreamManager extends UpstreamManager {

  @Override
  public void updateUpstream(String name, String configString) {
  }

  @Override
  public Set<Monitoring> getMonitoring() {
    return Set.of();
  }

  @Override
  public Upstream getUpstream(String serviceName, String profile) {
    return null;
  }

  @Override
  public UpstreamProfileSelector getProfileSelector(HttpClientContext ctx) {
    return UpstreamProfileSelector.EMPTY;
  }
}
