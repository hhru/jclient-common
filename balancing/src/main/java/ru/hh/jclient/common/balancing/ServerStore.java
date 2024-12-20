package ru.hh.jclient.common.balancing;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface ServerStore {
  Set<Server> getServers(String upstreamName);
  Optional<Integer> getInitialSize(String serviceName);
  void updateServers(String serviceName, Collection<Server> aliveServers, Collection<Server> deadServers);
}
