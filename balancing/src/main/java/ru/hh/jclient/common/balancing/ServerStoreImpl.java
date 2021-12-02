package ru.hh.jclient.common.balancing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ServerStoreImpl implements ServerStore {
  private final ConcurrentMap<String, List<Server>> serverList = new ConcurrentHashMap<>();
  private final Map<String, Integer> initialCapacities = new HashMap<>();

  @Override
  public List<Server> getServers(String serviceName) {
    List<Server> servers = serverList.get(serviceName);
    if (servers == null) {
      return List.of();
    }
    return servers;
  }

  @Override
  public Optional<Integer> getInitialSize(String serviceName) {
    return Optional.ofNullable(initialCapacities.get(serviceName));
  }

  @Override
  public void updateServers(String serviceName, List<Server> aliveServers) {
    serverList.compute(serviceName, (upstream, serverList) -> {
      if (serverList == null) {
        initialCapacities.put(serviceName, aliveServers.size());
      }
      return aliveServers;
    });
  }
}
