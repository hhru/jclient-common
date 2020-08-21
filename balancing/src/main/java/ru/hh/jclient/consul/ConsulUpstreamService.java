package ru.hh.jclient.consul;

import ru.hh.jclient.common.balancing.Server;

import java.util.List;

public interface ConsulUpstreamService {
  void registerUpstream(String serviceName, boolean allowCrossDC); //сделать регу

  List<Server> getServers(String serviceName);
}
