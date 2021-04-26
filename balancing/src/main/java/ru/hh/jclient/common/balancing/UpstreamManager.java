package ru.hh.jclient.common.balancing;

import ru.hh.jclient.common.Monitoring;

import java.util.Collection;
import java.util.Set;

public interface UpstreamManager {
  Upstream getUpstream(String serviceName);

  Set<Monitoring> getMonitoring();

  void updateUpstreams(Collection<String> upstreams);
}
