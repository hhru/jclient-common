package ru.hh.jclient.common.balancing;

import java.util.Collection;
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
    return Optional
        .ofNullable(serverList.get(serviceName))
        .map(List::copyOf)
        .orElseGet(List::of);
  }

  @Override
  public Optional<Integer> getInitialSize(String serviceName) {
    return Optional.ofNullable(initialCapacities.get(serviceName));
  }

  @Override
  public void updateServers(String serviceName, Collection<Server> aliveServers, Collection<Server> deadServers) {
    serverList.compute(serviceName, (upstream, serverSet) -> {
      if (serverSet != null) {
        serverSet.addAll(aliveServers);
        serverSet.removeAll(deadServers);
        return serverSet;
      }
      serverSet = ConcurrentHashMap.newKeySet();

      serverSet.addAll(aliveServers);
      initialCapacities.put(serviceName, aliveServers.size());
      return serverSet;
    });
  }
}
