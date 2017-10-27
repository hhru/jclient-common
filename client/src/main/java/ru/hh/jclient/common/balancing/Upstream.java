package ru.hh.jclient.common.balancing;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Upstream {

  private final String name;
  private final UpstreamConfig upstreamConfig;
  private final ScheduledExecutorService periodicTasksExecutor;
  private final AtomicInteger lastServerIndex = new AtomicInteger(0);

  private final ReadWriteLock configReadWriteLock = new ReentrantReadWriteLock();
  private final Lock configWriteLock = configReadWriteLock.writeLock();
  private final Lock configReadLock = configReadWriteLock.readLock();

  Upstream(String name, UpstreamConfig upstreamConfig, ScheduledExecutorService periodicTasksExecutor) {
    this.name = name;
    this.upstreamConfig = upstreamConfig;
    this.periodicTasksExecutor = periodicTasksExecutor;
  }

  int acquireServer(Set<Integer> excludedServers) {
    configReadLock.lock();
    try {
      List<Server> servers = upstreamConfig.getServers();
      int prevIndex, minIndex;
      do {
        prevIndex = lastServerIndex.get();
        minIndex = getLeastLoadedServer(servers, excludedServers, prevIndex);
      } while (!lastServerIndex.compareAndSet(prevIndex, minIndex));
      if (minIndex >= 0) {
        servers.get(minIndex).getCounter().onAcquire();
      }
      return minIndex;
    } finally {
      configReadLock.unlock();
    }
  }

  int acquireServer() {
    return acquireServer(Collections.emptySet());
  }

  void releaseServer(int serverIndex, boolean isError) {
    configReadLock.lock();
    try {
      Optional<Server> server = upstreamConfig.getServer(serverIndex);
      if (server.isPresent()) {
        server.get().getCounter().onRelease(isError);
        if (isError) {
          int currentFails = server.get().getCounter().getFails();
          if (upstreamConfig.getMaxFails() > 0 && currentFails >= upstreamConfig.getMaxFails()) {
            server.get().suspend(upstreamConfig.getFailTimeoutMs(), periodicTasksExecutor);
          }
        }
      }
    } finally {
      configReadLock.unlock();
    }
  }

  private int getLeastLoadedServer(List<Server> servers, Set<Integer> excludedServers, int lastIndex) {
    int minIndex = -1;
    int numOfServers = servers.size();
    float minLoad = 0;
    if (lastIndex < 0) {
      lastIndex = 0;
    }
    for (int i = 0; i < numOfServers; i++) {
      int index = (lastIndex + i) % numOfServers;
      Server server = servers.get(index);
      if (server != null && server.isActive() && !excludedServers.contains(index)) {
        int requests = server.getCounter().getRequests();
        float load = (float) requests / server.getWeight();
        if (minIndex < 0 || load < minLoad) {
          minIndex = index;
          minLoad = load;
        }
      }
    }
    return minIndex;
  }

  void updateConfig(UpstreamConfig newConfig) {
    configWriteLock.lock();
    try {
      upstreamConfig.update(newConfig);
    } finally {
      configWriteLock.unlock();
    }
  }

  String getServerAddress(int index) {
    configReadLock.lock();
    try {
      Optional<Server> server = upstreamConfig.getServer(index);
      return server.map(Server::getAddress).orElse(null);
    } finally {
      configReadLock.unlock();
    }
  }

  ServerCounter getServerCounter(int index) {
    configReadLock.lock();
    try {
      Optional<Server> server = upstreamConfig.getServer(index);
      return server.map(Server::getCounter).orElse(null);
    } finally {
      configReadLock.unlock();
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
