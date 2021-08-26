package ru.hh.jclient.common.balancing;

import java.util.List;

public class UpstreamMorozov extends Upstream {

  public UpstreamMorozov(String upstreamName, UpstreamConfigs upstreamConfigs, List<Server> servers, String datacenter,
                         boolean allowCrossDCRequests, boolean enabled) {
    super(upstreamName, upstreamConfigs, servers, datacenter, allowCrossDCRequests, enabled);
  }
}
