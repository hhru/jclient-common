package ru.hh.jclient.common.balancing;

import static java.util.stream.Collectors.toList;
import static ru.hh.jclient.common.balancing.BalancingStrategy.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public final class Upstream {
  private final String name;
  private final UpstreamConfig upstreamConfig;
  private final ScheduledExecutorService scheduledExecutor;
  private final String datacenter;
  private final boolean allowCrossDCRequests;

  private final ReadWriteLock configReadWriteLock = new ReentrantReadWriteLock();
  private final Lock configWriteLock = configReadWriteLock.writeLock();
  private final Lock configReadLock = configReadWriteLock.readLock();

  Upstream(String name, UpstreamConfig upstreamConfig, ScheduledExecutorService scheduledExecutor) {
    this(name, upstreamConfig, scheduledExecutor, null, false);
  }

  Upstream(String name, UpstreamConfig upstreamConfig, ScheduledExecutorService scheduledExecutor, String datacenter, boolean allowCrossDCRequests) {
    this.name = name;
    this.upstreamConfig = upstreamConfig;
    this.scheduledExecutor = scheduledExecutor;
    this.datacenter = datacenter;
    this.allowCrossDCRequests = allowCrossDCRequests;
  }

  ServerEntry acquireServer(Set<Integer> excludedServers) {
    try {
      configReadLock.lock();
      List<Server> servers = upstreamConfig.getServers();
      int index = getLeastLoadedServer(servers, excludedServers, datacenter, allowCrossDCRequests);
      if (index >= 0) {
        Server server = servers.get(index);
        server.acquire();
        return new ServerEntry(index, server.getAddress(), server.getRack(), server.getDatacenter());
      }
      return null;
    } finally {
      configReadLock.unlock();
    }
  }

  List<ServerEntry> acquireAdaptiveServers(int retriesCount) {
    try {
      configReadLock.lock();
      List<Server> servers = upstreamConfig.getServers();
      List<Server> allowedServers = new ArrayList<>();
      List<Integer> allowedIds = new ArrayList<>();
      for (int i = 0; i < servers.size(); i++) {
        Server server = servers.get(i);
        if (allowCrossDCRequests || Objects.equals(datacenter, server.getDatacenter())) {
          allowedIds.add(i);
          allowedServers.add(server);
        }
      }

      return AdaptiveBalancingStrategy
          .getServers(allowedServers, retriesCount)
          .stream()
          .map(id -> {
            Server server = allowedServers.get(id);
            return new ServerEntry(allowedIds.get(id), server.getAddress(), server.getRack(), server.getDatacenter());
          })
          .collect(toList());
    } finally {
      configReadLock.unlock();
    }
  }

  ServerEntry acquireServer() {
    return acquireServer(Collections.emptySet());
  }

  void releaseServer(int serverIndex, boolean isError, long responseTimeMs) {
    configReadLock.lock();
    try {
      List<Server> servers = upstreamConfig.getServers();
      if (serverIndex < 0 || serverIndex >= servers.size()) {
        return;
      }
      Server server = servers.get(serverIndex);
      if (server != null) {
        server.release(isError, responseTimeMs);
        if (isError) {
          if (upstreamConfig.getMaxFails() > 0 && server.getFails() >= upstreamConfig.getMaxFails()) {
            server.deactivate(upstreamConfig.getFailTimeoutMs(), scheduledExecutor);
          }
        }
      }

      rescale(servers);
    } finally {
      configReadLock.unlock();
    }
  }

  private void rescale(List<Server> servers) {
    boolean[] rescale = new boolean[] {true, allowCrossDCRequests};
    iterateServers(servers, s -> {
      int localOrRemote = Objects.equals(s.getDatacenter(), datacenter) ? 0 : 1;
      rescale[localOrRemote] = rescale[localOrRemote] && s.getStatsRequests() >= s.getWeight();
    });

    if (rescale[0] || rescale[1]) {
      iterateServers(servers, s -> {
        int localOrRemote = Objects.equals(s.getDatacenter(), datacenter) ? 0 : 1;
        if (rescale[localOrRemote]) {
          s.rescaleStatsRequests();
        }
      });
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

  private static void iterateServers(List<Server> servers, Consumer<Server> function) {
    servers.forEach(s -> {
      if (s == null || !s.isActive()) {
        return;
      }

      function.accept(s);
    });
  }
}
