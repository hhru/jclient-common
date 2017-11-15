package ru.hh.jclient.common.balancing;

import static ru.hh.jclient.common.balancing.BalancingStrategy.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Upstream {

  private final String name;
  private final UpstreamConfig upstreamConfig;
  private final ScheduledExecutorService scheduledExecutor;

  private final ReadWriteLock configReadWriteLock = new ReentrantReadWriteLock();
  private final Lock configWriteLock = configReadWriteLock.writeLock();
  private final Lock configReadLock = configReadWriteLock.readLock();

  Upstream(String name, UpstreamConfig upstreamConfig, ScheduledExecutorService scheduledExecutor) {
    this.name = name;
    this.upstreamConfig = upstreamConfig;
    this.scheduledExecutor = scheduledExecutor;
  }

  ServerEntry acquireServer(Set<Integer> excludedServers) {
    try {
      configReadLock.lock();
      List<Server> servers = upstreamConfig.getServers();
      int index = getLeastLoadedServer(servers, excludedServers);
      if (index >= 0) {
        Server server = servers.get(index);
        server.acquire();
        return new ServerEntry(index, server.getAddress());
      }
      return null;
    } finally {
      configReadLock.unlock();
    }
  }

  ServerEntry acquireServer() {
    return acquireServer(Collections.emptySet());
  }

  void releaseServer(int serverIndex, boolean isError) {
    configReadLock.lock();
    try {
      List<Server> servers = upstreamConfig.getServers();
      if (serverIndex < 0 || serverIndex >= servers.size()) {
        return;
      }
      Server server = servers.get(serverIndex);
      if (server != null) {
        server.release(isError);
        if (isError) {
          if (upstreamConfig.getMaxFails() > 0 && server.getFails() >= upstreamConfig.getMaxFails()) {
            server.deactivate(upstreamConfig.getFailTimeoutMs(), scheduledExecutor);
          }
        }
      }
      checkAndResetStats(servers);
    } finally {
      configReadLock.unlock();
    }
  }

  void updateConfig(UpstreamConfig newConfig) {
    configWriteLock.lock();
    try {
      upstreamConfig.update(newConfig);
    } finally {
      configWriteLock.unlock();
    }
  }

  private static void checkAndResetStats(List<Server> servers) {
    if (servers.stream().allMatch(server -> server.getStatsRequests() >= server.getWeight())) {
      servers.forEach(Server::resetStatsRequests);
    }
  }

  String getName() {
    return name;
  }

  UpstreamConfig getConfig() {
    configReadLock.lock();
    try {
      return upstreamConfig;
    } finally {
      configReadLock.unlock();
    }
  }
}
