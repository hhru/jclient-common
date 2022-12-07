package ru.hh.jclient.common.balancing;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.Monitoring;

public class BalancingUpstreamManager implements UpstreamManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(BalancingUpstreamManager.class);

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
    List<Server> servers = serverStore.getServers(upstreamName);

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
        upstream.update(newConfig, servers);
      }
      return upstream;
    });
  }

  private Upstream createUpstream(String upstreamName, UpstreamConfigs upstreamConfigs, List<Server> servers) {
    return new Upstream(upstreamName, upstreamConfigs, servers, datacenter, allowCrossDCRequests);
  }

  @Override
  public Upstream getUpstream(String name) {
    return upstreams.get(name);
  }

  @Override
  public Set<Monitoring> getMonitoring() {
    return Set.copyOf(monitoring);
  }

  Map<String, Upstream> getUpstreams() {
    return upstreams;
  }
}
