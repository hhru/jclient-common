package ru.hh.jclient.common;

import ru.hh.jclient.common.balancing.Upstream;
import ru.hh.jclient.common.balancing.UpstreamMonitoring;

import java.util.Map;

public interface UpstreamManager {
  Upstream getUpstream(String host);

  Map<String, Upstream> getUpstreams();

  void updateUpstream(String name, String configString);

  UpstreamMonitoring getMonitoring();
}
