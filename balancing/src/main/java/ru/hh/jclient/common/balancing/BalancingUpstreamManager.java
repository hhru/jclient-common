package ru.hh.jclient.common.balancing;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.balancing.config.ApplicationConfig;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
  private final ValidationSettings validationSettings;

public BalancingUpstreamManager(ConfigStore configStore,
                                ServerStore serverStore,
                                Set<Monitoring> monitoring,
                                JClientInfrastructureConfig infrastructureConfig,
                                boolean allowCrossDCRequests,
                                ValidationSettings validationSettings) {
    this.monitoring = requireNonNull(monitoring, "monitorings must not be null");
    this.datacenter = infrastructureConfig.getCurrentDC() == null ? null : infrastructureConfig.getCurrentDC();
    this.allowCrossDCRequests = allowCrossDCRequests;
    this.serverStore = serverStore;
    this.configStore = configStore;
    this.validationSettings = validationSettings;
  }

  @Override
  public void updateUpstreams(Collection<String> upstreams) {
    upstreams.forEach(this::updateUpstream);
  }

  private void updateUpstream(@Nonnull String upstreamName) {

    ApplicationConfig upstreamConfig = configStore.getUpstreamConfig(upstreamName);
    var newConfig = ApplicationConfig.toUpstreamConfigs(upstreamConfig, UpstreamConfig.DEFAULT);
    List<Server> servers = serverStore.getServers(upstreamName);
    Optional<Integer> minAllowedSize = serverStore.getInitialSize(upstreamName)
      .map(initialCapacity -> (int) Math.ceil(initialCapacity * (1 - validationSettings.allowedDegradationPart)));


    if (minAllowedSize.isPresent() && servers.size() < minAllowedSize.get()) {
      monitoring.forEach(m -> m.countUpdateIgnore(upstreamName, datacenter));
      LOGGER.warn("Ignoring update which contains {} servers, for upstream {} allowed minimum is {}",
        LOGGER.isDebugEnabled() ? servers : servers.size(),
        upstreamName,
        minAllowedSize
      );
      return;
    }
    upstreams.compute(upstreamName, (serviceName, upstream) -> {
      if (upstream == null) {
        upstream = createUpstream(upstreamName, newConfig, servers);
      } else {
        upstream.updateConfig(newConfig, servers);
      }
      return upstream;
    });
  }

  private Upstream createUpstream(String upstreamName, Map<String, UpstreamConfig>  config, List<Server> servers) {
    return new Upstream(upstreamName, config, servers, datacenter, allowCrossDCRequests, true);
  }

  @Override
  public Upstream getUpstream(String serviceName) {
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

  public static class ValidationSettings {
    private double allowedDegradationPart = 0.5d;

    public ValidationSettings setAllowedDegradationPart(double allowedDegradationPart) {
      this.allowedDegradationPart = allowedDegradationPart;
      return this;
    }
  }
}
