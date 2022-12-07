package ru.hh.jclient.common.balancing;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import static java.util.stream.Collectors.toList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static ru.hh.jclient.common.balancing.BalancingStrategy.getLeastLoadedServer;

public class Upstream {
  private static final Logger LOGGER = LoggerFactory.getLogger(Upstream.class);
  static final String DEFAULT_PROFILE = "default";
  static final int DEFAULT_STAT_LIMIT = 10_000_000;

  private final String name;
  private final String datacenter;
  private int statLimit = DEFAULT_STAT_LIMIT;
  private final boolean allowCrossDCRequests;

  private volatile List<Server> servers;
  private volatile UpstreamConfigs upstreamConfigs;
  private boolean failedSelection = false;

  private final ReadWriteLock configReadWriteLock = new ReentrantReadWriteLock();
  private final Lock configWriteLock = configReadWriteLock.writeLock();
  private final Lock configReadLock = configReadWriteLock.readLock();

  private final StampedLock lock = new StampedLock();

  Upstream(String name,
           UpstreamConfigs upstreamConfigs,
           List<Server> servers,
           String datacenter,
           boolean allowCrossDCRequests) {
    this.name = name;
    this.datacenter = datacenter;
    this.allowCrossDCRequests = allowCrossDCRequests;
    this.update(upstreamConfigs, servers);
  }

  List<Server> getServers() {
    return servers;
  }

  ServerEntry acquireServer(Set<Integer> excludedServers) {
    configReadLock.lock();
    try {
      int index;
      List<Server> servers = this.servers;
      long readStamp = lock.tryOptimisticRead();
      index = getLeastLoadedServer(servers, excludedServers, datacenter, allowCrossDCRequests, Clock.systemDefaultZone());
      if (!lock.validate(readStamp)) {
        //fallback to lock
        readStamp = lock.readLock();
        try {
          index = getLeastLoadedServer(servers, excludedServers, datacenter, allowCrossDCRequests, Clock.systemDefaultZone());
        } finally {
          lock.unlockRead(readStamp);
        }
      }

      if (index >= 0) {
        Server server = servers.get(index);
        server.acquire();
        failedSelection = false;
        return new ServerEntry(index, server.getAddress(), server.getDatacenter());
      }
      if (!failedSelection) {
        failedSelection = true;
        LOGGER.warn("Next server for upstream {} with excluded server indexes {} not found. Returning null", this, excludedServers);
      }
      return null;
    } finally {
      configReadLock.unlock();
    }
  }

  List<ServerEntry> acquireAdaptiveServers(String profile) {
    configReadLock.lock();
    try {
      List<Server> allowedServers = new ArrayList<>();
      List<Integer> allowedIds = new ArrayList<>();
      for (int i = 0; i < servers.size(); i++) {
        Server server = servers.get(i);
        if (server != null && (allowCrossDCRequests || Objects.equals(datacenter, server.getDatacenter()))) {
          allowedIds.add(i);
          allowedServers.add(server);
        }
      }

      return AdaptiveBalancingStrategy
          .getServers(allowedServers, getConfig(profile).getMaxTries())
          .stream()
          .map(id -> {
            Server server = allowedServers.get(id);
            return new ServerEntry(allowedIds.get(id), server.getAddress(), server.getDatacenter());
          })
          .collect(toList());
    } finally {
      configReadLock.unlock();
    }
  }

  ServerEntry acquireServer() {
    return acquireServer(Set.of());
  }

  void releaseServer(int serverIndex, boolean isRetry, boolean isError, long responseTimeMicros) {
    releaseServer(serverIndex, isRetry, isError, responseTimeMicros, false);
  }

  void releaseServer(int serverIndex, boolean isRetry, boolean isError, long responseTimeMicros, boolean adaptive) {
    configReadLock.lock();
    try {
      if (serverIndex < 0 || serverIndex >= servers.size()) {
        return;
      }
      Server server = servers.get(serverIndex);
      if (server != null) {
        if (adaptive) {
          server.releaseAdaptive(isError, responseTimeMicros);
        } else {
          server.release(isRetry);
        }
      }

      if (!adaptive) {
        rescale(servers);
      }
    } finally {
      configReadLock.unlock();
    }
  }

  private void rescale(List<Server> servers) {
    boolean[] rescale = {true, allowCrossDCRequests};
    iterateServers(servers, server -> {
      int localOrRemote = Objects.equals(server.getDatacenter(), datacenter) ? 0 : 1;
      rescale[localOrRemote] &= server.needToRescale();
    });

    if (rescale[0] || rescale[1]) {
      LOGGER.trace("Need to rescale servers. Double checking with lock");
      long writeStamp = lock.writeLock();
      try {
        iterateServers(servers, server -> {
          int localOrRemote = Objects.equals(server.getDatacenter(), datacenter) ? 0 : 1;
          rescale[localOrRemote] &= server.needToRescale();
        });
        if (rescale[0] || rescale[1]) {
          LOGGER.debug("Rescaling servers {}", servers);
          iterateServers(servers, server -> {
            int localOrRemote = Objects.equals(server.getDatacenter(), datacenter) ? 0 : 1;
            if (rescale[localOrRemote]) {
              server.rescaleStatsRequests();
            }
          });
        }
      } finally {
        lock.unlockWrite(writeStamp);
      }
    }
  }

  void update(UpstreamConfigs newConfigs, List<Server> servers) {
    configWriteLock.lock();
    try {
      this.upstreamConfigs = newConfigs;
      this.servers = servers;
      this.servers.forEach(server -> {
        server.setStatLimit(statLimit);
        server.setSharedLock(lock);
      });
      UpstreamConfig upstreamConfig = getUpstreamConfigOrThrow(DEFAULT_PROFILE);
      initSlowStart(upstreamConfig, Clock.systemDefaultZone());
      this.failedSelection = false;
    } finally {
      configWriteLock.unlock();
    }
  }

  private UpstreamConfig getUpstreamConfigOrThrow(String profile) {
    return upstreamConfigs.get(profile).orElseThrow(() -> new IllegalStateException("Profile " + profile + " should be present"));
  }

  private void initSlowStart(UpstreamConfig upstreamConfig, Clock clock) {
    servers.forEach(server -> server.setSlowStartEndTimeIfNeeded(upstreamConfig.getSlowStartIntervalSec(), clock));
  }

  String getName() {
    return name;
  }

  public String getDatacenter() {
    return datacenter;
  }

  UpstreamConfig getConfig(String profile) {
    profile = profile == null || profile.isEmpty() ? DEFAULT_PROFILE : profile;
    configReadLock.lock();
    try {
      return getUpstreamConfigOrThrow(profile);
    } finally {
      configReadLock.unlock();
    }
  }

  //visible for testing
  void setStatLimit(int statLimit) {
    this.statLimit = statLimit;
    servers.forEach(server -> server.setStatLimit(statLimit));
  }

  private static void iterateServers(List<Server> servers, Consumer<Server> function) {
    servers.forEach(server -> {
      if (server == null) {
        return;
      }

      function.accept(server);
    });
  }

  @Override
  public String toString() {
    return "Upstream{" +
      "name=" + name +
      ", upstreamConfig=" + upstreamConfigs +
      ", datacenter='" + datacenter + '\'' +
      ", allowCrossDCRequests=" + allowCrossDCRequests +
      ", servers=" + servers +
      '}';
  }
}
