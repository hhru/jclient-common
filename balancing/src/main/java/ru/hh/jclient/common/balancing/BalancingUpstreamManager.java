package ru.hh.jclient.common.balancing;

import java.util.Collection;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.Monitoring;

public class BalancingUpstreamManager implements UpstreamManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(BalancingUpstreamManager.class);
  private static final int SCHEMA_SEPARATOR_LEN = 3;

  public static final String SCHEMA_SEPARATOR = "://";

  private final ConfigStore configStore;
  private final ServerStore serverStore;

  private final Map<String, Upstream> upstreams = new ConcurrentHashMap<>();
  private final Set<Monitoring> monitoring;
  private final String datacenter;
  private final boolean allowCrossDCRequests;

  public BalancingUpstreamManager(ConfigStore configStore,
                                  ServerStore serverStore,
                                  Set<Monitoring> monitoring,
                                  JClientInfrastructureConfig infrastructureConfig,
                                  boolean allowCrossDCRequests) {
    this.monitoring = requireNonNull(monitoring, "monitorings must not be null");
    this.datacenter = infrastructureConfig.getCurrentDC() == null ? null : infrastructureConfig.getCurrentDC();
    this.allowCrossDCRequests = allowCrossDCRequests;
    this.serverStore = serverStore;
    this.configStore = configStore;
  }

  @Override
  public void updateUpstreams(Collection<String> upstreams) {
    upstreams.forEach(this::updateUpstream);
  }

  private void updateUpstream(@Nonnull String upstreamName) {
    Set<Server> servers = serverStore.getServers(upstreamName);

    if (servers.isEmpty() && serverStore.getInitialSize(upstreamName).filter(val -> val > 0).isPresent()) {
      monitoring.forEach(m -> m.countUpdateIgnore(upstreamName, datacenter));
      LOGGER.warn("Ignoring empty update for upstream {}", upstreamName);
      return;
    }

    var newConfig = configStore.getUpstreamConfig(upstreamName);
    if (newConfig == null) {
      LOGGER.debug("Config for upstream {} is not found", upstreamName);
      return;
    }
    upstreams.compute(upstreamName, (serviceName, upstream) -> {
      if (upstream == null) {
        upstream = createUpstream(upstreamName, newConfig, servers);
      } else {
        // TODO REPLACE servers.stream().toList() WITH SET
        upstream.update(newConfig, servers.stream().toList());
      }
      return upstream;
    });
  }

  private Upstream createUpstream(String upstreamName, UpstreamConfigs upstreamConfigs, Set<Server> servers) {
    // TODO REPLACE servers.stream().toList() WITH SET
    return new Upstream(upstreamName, upstreamConfigs, servers.stream().toList(), datacenter, allowCrossDCRequests);
  }

  @Override
  public Upstream getUpstream(String serviceName, @Nullable String profile) {
    return upstreams.get(getNameWithoutScheme(serviceName));
  }

  @Override
  public Set<Monitoring> getMonitoring() {
    return Set.copyOf(monitoring);
  }

  static String getNameWithoutScheme(String host) {
    int beginIndex = host.indexOf(SCHEMA_SEPARATOR) + SCHEMA_SEPARATOR_LEN;
    return beginIndex > 2 ? host.substring(beginIndex) : host;
  }

  Map<String, Upstream> getUpstreams() {
    return upstreams;
  }
}
