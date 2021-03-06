package ru.hh.jclient.common.balancing;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;
import static ru.hh.jclient.common.balancing.BalancingStrategy.getLeastLoadedServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class Upstream {
  private static final Logger LOGGER = LoggerFactory.getLogger(Upstream.class);
  static final String DEFAULT_PROFILE = "default";

  private final String upstreamName;
  private final String datacenter;
  private final boolean allowCrossDCRequests;
  private final boolean enabled;

  private volatile List<Server> servers;
  private volatile UpstreamConfigs upstreamConfigs;
  private boolean failedSelection = false;

  private final ReadWriteLock configReadWriteLock = new ReentrantReadWriteLock();
  private final Lock configWriteLock = configReadWriteLock.writeLock();
  private final Lock configReadLock = configReadWriteLock.readLock();

  Upstream(String upstreamName,
           UpstreamConfigs upstreamConfigs,
           List<Server> servers,
           String datacenter,
           boolean allowCrossDCRequests,
           boolean enabled) {
    this.upstreamName = upstreamName;
    this.datacenter = datacenter;
    this.allowCrossDCRequests = allowCrossDCRequests;
    this.enabled = enabled;
    this.updateConfig(upstreamConfigs, servers);
  }

  List<Server> getServers() {
    return servers;
  }

  ServerEntry acquireServer(Set<Integer> excludedServers) {
    configReadLock.lock();
    try {
      int index = getLeastLoadedServer(servers, excludedServers, datacenter, allowCrossDCRequests, Clock.systemDefaultZone());
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

  void releaseServer(int serverIndex, boolean isError, long responseTimeMicros) {
    releaseServer(serverIndex, isError, responseTimeMicros, false);
  }

  void releaseServer(int serverIndex, boolean isError, long responseTimeMicros, boolean adaptive) {
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
          server.release(isError);
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
      rescale[localOrRemote] &= server.getStatsRequests() >= server.getWeight();
    });

    if (rescale[0] || rescale[1]) {
      iterateServers(servers, server -> {
        int localOrRemote = Objects.equals(server.getDatacenter(), datacenter) ? 0 : 1;
        if (rescale[localOrRemote]) {
          server.rescaleStatsRequests();
        }
      });
    }
  }

  void updateConfig(UpstreamConfigs newConfigs, List<Server> servers) {
    configWriteLock.lock();
    try {
      this.upstreamConfigs = newConfigs;
      this.servers = servers;
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
    return upstreamName;
  }

  public boolean isEnabled() {
    return enabled;
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
      "upstreamName=" + upstreamName +
      ", upstreamConfig=" + upstreamConfigs +
      ", datacenter='" + datacenter + '\'' +
      ", allowCrossDCRequests=" + allowCrossDCRequests +
      ", enabled=" + enabled +
      ", servers=" + servers +
      '}';
  }
}
