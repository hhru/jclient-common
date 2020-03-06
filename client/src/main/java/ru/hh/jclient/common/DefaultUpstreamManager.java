package ru.hh.jclient.common;

import java.util.Set;

final class DefaultUpstreamManager extends UpstreamManager {

  @Override
  public void updateUpstream(String name, String configString) {
  }

  @Override
  public Set<Monitoring> getMonitoring() {
    return Set.of();
  }
}
