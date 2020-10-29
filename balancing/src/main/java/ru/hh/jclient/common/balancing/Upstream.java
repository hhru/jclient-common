package ru.hh.jclient.common.balancing;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static ru.hh.jclient.common.balancing.BalancingStrategy.getLeastLoadedServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Upstream {
  private final UpstreamKey upstreamKey;
  private final UpstreamConfig upstreamConfig;
  private final ScheduledExecutorService scheduledExecutor;
  private final String datacenter;
  private final boolean allowCrossDCRequests;
  private final boolean enabled;
  private volatile List<Server> servers;

  private final ReadWriteLock configReadWriteLock = new ReentrantReadWriteLock();
  private final Lock configWriteLock = configReadWriteLock.writeLock();
  private final Lock configReadLock = configReadWriteLock.readLock();

  Upstream(String upstreamName, UpstreamConfig upstreamConfig, List<Server> servers, ScheduledExecutorService scheduledExecutor) {
    this(UpstreamKey.ofComplexName(upstreamName), upstreamConfig, servers, scheduledExecutor, null, false, true);
  }

  Upstream(UpstreamKey upstreamKey,
           UpstreamConfig upstreamConfig,
           List<Server> servers,
           ScheduledExecutorService scheduledExecutor,
           String datacenter,
           boolean allowCrossDCRequests,
           boolean enabled) {
    this.upstreamKey = upstreamKey;
    this.upstreamConfig = upstreamConfig;
    this.servers = servers;
    this.scheduledExecutor = scheduledExecutor;
    this.datacenter = datacenter == null ? null : datacenter.toLowerCase();
    this.allowCrossDCRequests = allowCrossDCRequests;
    this.enabled = enabled;
  }

  ServerEntry acquireServer(Set<Integer> excludedServers) {
    configReadLock.lock();
    try {
      int index = getLeastLoadedServer(servers, excludedServers, datacenter, allowCrossDCRequests);
      if (index >= 0) {
        Server server = servers.get(index);
        server.acquire();
        return new ServerEntry(index, server.getAddress(), server.getDatacenter());
      }
      return null;
    } finally {
      configReadLock.unlock();
    }
  }

  List<ServerEntry> acquireAdaptiveServers(int retriesCount) {
    configReadLock.lock();
    try {
      List<Server> allowedServers = new ArrayList<>();
      List<Integer> allowedIds = new ArrayList<>();
      for (int i = 0; i < servers.size(); i++) {
        Server server = servers.get(i);
        if (server != null && (allowCrossDCRequests || Objects.equals(datacenter, server.getDatacenterLowerCased()))) {
          allowedIds.add(i);
          allowedServers.add(server);
        }
      }

      return AdaptiveBalancingStrategy
          .getServers(allowedServers, retriesCount)
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

  ServerEntry acquireServer(List<Server> servers) {
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
          server.release(isError, responseTimeMicros);
          if (isError) {
            if (upstreamConfig.getMaxFails() > 0 && server.getFails() >= upstreamConfig.getMaxFails()) {
              server.deactivate(upstreamConfig.getFailTimeoutMs(), scheduledExecutor);
            }
          }
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
      int localOrRemote = Objects.equals(server.getDatacenterLowerCased(), datacenter) ? 0 : 1;
      rescale[localOrRemote] &= server.getStatsRequests() >= server.getWeight();
    });

    if (rescale[0] || rescale[1]) {
      iterateServers(servers, server -> {
        int localOrRemote = Objects.equals(server.getDatacenterLowerCased(), datacenter) ? 0 : 1;
        if (rescale[localOrRemote]) {
          server.rescaleStatsRequests();
        }
      });
    }
  }

  void updateConfig(UpstreamConfig newConfig, List<Server> servers) {
    configWriteLock.lock();
    try {
      this.upstreamConfig.update(newConfig);
      this.servers = servers;
    } finally {
      configWriteLock.unlock();
    }
  }

  String getName() {
    return upstreamKey.getWholeName();
  }

  public UpstreamKey getKey() {
    return upstreamKey;
  }

  public boolean isEnabled() {
    return enabled;
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
    servers.forEach(server -> {
      if (server == null || !server.isActive()) {
        return;
      }

      function.accept(server);
    });
  }

  public static final class UpstreamKey {
    private static final String SEP = ":";
    private final String serviceName;
    private final String profileName;

    public static UpstreamKey ofComplexName(String wholeName) {
      String[] parts = wholeName.split(SEP, 2);
      return new UpstreamKey(parts[0], parts.length == 2 ? parts[1] : null);
    }

    public UpstreamKey(String serviceName, @Nullable String profileName) {
      this.serviceName = requireNonNull(serviceName);
      this.profileName = UpstreamGroup.DEFAULT_PROFILE.equals(profileName) ? null : profileName;
    }

    public String getServiceName() {
      return serviceName;
    }

    public String getProfileName() {
      return profileName;
    }

    public String getWholeName() {
      return Stream.of(serviceName, profileName).filter(Objects::nonNull).collect(Collectors.joining(SEP));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      var thatKey = (UpstreamKey) o;
      return Objects.equals(serviceName, thatKey.serviceName) &&
          Objects.equals(profileName, thatKey.profileName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(serviceName, profileName);
    }

    @Override
    public String toString() {
      return "UpstreamKey{" + getWholeName() + '}';
    }
  }
}
