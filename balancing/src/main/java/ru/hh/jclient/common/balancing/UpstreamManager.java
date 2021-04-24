package ru.hh.jclient.common.balancing;

import ru.hh.jclient.common.Monitoring;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

public interface UpstreamManager {
  Upstream getUpstream(String serviceName, @Nullable String profile);

  default Upstream getUpstream(String serviceName) {
    return getUpstream(serviceName, null);
  }

  Set<Monitoring> getMonitoring();

  void updateUpstreams(Collection<String> upstreams, boolean throwOnUpstreamValidation);
}
