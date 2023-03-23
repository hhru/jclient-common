package ru.hh.jclient.common.balancing;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServerStore {
  List<Server> getServers(String serviceName);
  Optional<Integer> getInitialSize(String serviceName);
  void updateServers(String serviceName, Collection<Server> aliveServers, Collection<Server> deadServers);
}
