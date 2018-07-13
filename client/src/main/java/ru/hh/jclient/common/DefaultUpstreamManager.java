package ru.hh.jclient.common;

import ru.hh.jclient.common.balancing.Upstream;

import java.util.Collections;
import java.util.Map;

final class DefaultUpstreamManager extends UpstreamManager {
  @Override
  public Upstream getUpstream(String host) {
    return null;
  }

  @Override
  public Map<String, Upstream> getUpstreams() {
    return Collections.emptyMap();
  }

  @Override
  public void updateUpstream(String name, String configString) {
  }

  @Override
  public Monitoring getMonitoring() {
    return null;
  }
}
