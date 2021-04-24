package ru.hh.jclient.common.balancing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ServerStoreImpl implements ServerStore {
  private final ConcurrentMap<String, Set<Server>> serverList = new ConcurrentHashMap<>();
  private final Map<String, Integer> initialCapacities = new HashMap<>();

  @Override
  public List<Server> getServers(String serviceName) {
    Set<Server> servers = serverList.get(serviceName);
    if (servers == null) {
      return List.of();
    }
    return List.copyOf(servers);
  }

  @Override
  public Optional<Integer> getInitialSize(String serviceName) {
    return Optional.ofNullable(initialCapacities.get(serviceName));
  }

  @Override
  public void updateServers(String serviceName, Collection<Server> aliveServers, Collection<Server> deadServers) {
    Set<Server> currentServers = serverList.computeIfAbsent(serviceName, k -> {
      initialCapacities.putIfAbsent(serviceName, aliveServers.size());
      return Collections.newSetFromMap(new ConcurrentHashMap<>());
    });
    currentServers.addAll(aliveServers);
    currentServers.removeAll(deadServers);
  }

}
