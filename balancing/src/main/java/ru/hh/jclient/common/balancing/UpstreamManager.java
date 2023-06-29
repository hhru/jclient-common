package ru.hh.jclient.common.balancing;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Set;
import ru.hh.jclient.common.Monitoring;

public interface UpstreamManager {
  default Upstream getUpstream(String serviceName) {
    return getUpstream(serviceName, null);
  }

  Upstream getUpstream(String serviceName, @Nullable String profile);

  Set<Monitoring> getMonitoring();

  void updateUpstreams(Collection<String> upstreams);
}
