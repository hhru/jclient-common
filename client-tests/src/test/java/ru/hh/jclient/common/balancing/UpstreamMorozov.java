package ru.hh.jclient.common.balancing;

import java.util.List;

// to make constructor available outside of the package to housekeep tests better
public class UpstreamMorozov extends Upstream {

  public UpstreamMorozov(String upstreamName, UpstreamConfigs upstreamConfigs, List<Server> servers, String datacenter) {
    super(upstreamName, upstreamConfigs, servers, datacenter);
  }
}
