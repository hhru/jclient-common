package ru.hh.jclient.common.balancing;

import java.util.Collection;
import java.util.Set;
import ru.hh.jclient.common.Monitoring;

public interface UpstreamManager {
  Upstream getUpstream(String name);

  Set<Monitoring> getMonitoring();

  void updateUpstreams(Collection<String> upstreams);
}
