package ru.hh.jclient.common;

import ru.hh.jclient.common.balancing.Upstream;
import ru.hh.jclient.common.balancing.UpstreamMonitoring;

import java.util.Collections;
import java.util.Map;

class DefaultUpstreamManager implements UpstreamManager {
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
  public UpstreamMonitoring getMonitoring() {
    return null;
  }
}
