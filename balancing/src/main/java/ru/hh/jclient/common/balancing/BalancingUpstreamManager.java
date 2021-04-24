package ru.hh.jclient.common.balancing;

import static java.util.Objects.requireNonNull;
import static ru.hh.jclient.common.balancing.PropertyKeys.IGNORE_NO_SERVERS_IN_CURRENT_DC_KEY;
import static ru.hh.jclient.common.balancing.PropertyKeys.SYNC_UPDATE_KEY;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.balancing.config.ApplicationConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
  public void updateUpstreams(Collection<String> upstreams, boolean throwOnUpstreamValidation) {
    if (validationSettings.failOnEmptyUpstreams) {
      checkAllUpstreamConfigsExist(upstreams, throwOnUpstreamValidation);
      checkServersForAllUpstreamsExist(upstreams, throwOnUpstreamValidation);
    }
    if (!validationSettings.ignoreNoServersInCurrentDC) {
      checkServersForAllUpstreamsInCurrentDcExist(upstreams, throwOnUpstreamValidation);
    }
    upstreams.forEach(this::updateUpstream);
  }

  private void updateUpstream(@Nonnull String upstreamName) {
    var upstreamKey = Upstream.UpstreamKey.ofComplexName(upstreamName);

    ApplicationConfig upstreamConfig = configStore.getUpstreamConfig(upstreamKey.getServiceName());
    var newConfig = ApplicationConfig.toUpstreamConfigs(upstreamConfig, UpstreamConfig.DEFAULT);
    List<Server> servers = serverStore.getServers(upstreamName);
    int minAllowedSize = serverStore.getInitialSize(upstreamKey.getServiceName()).stream()
      .mapToInt(initialCapacity -> (int) Math.ceil(initialCapacity * (1 - validationSettings.allowedDegradationPart))).findFirst().orElse(-1);


    if (minAllowedSize > 0 && servers.size() < minAllowedSize) {
      monitoring.forEach(m -> m.countUpdateIgnore(upstreamName, datacenter));
      LOGGER.warn("Ignoring update which contains {} servers, for upstream {} allowed minimum is {}",
        LOGGER.isDebugEnabled() ? servers : servers.size(),
        upstreamKey.getServiceName(),
        minAllowedSize
      );
      return;
    }
    upstreams.compute(upstreamKey.getServiceName(), (serviceName, upstream) -> {
      if (upstream == null) {
        upstream = createUpstream(upstreamKey, newConfig, servers);
      } else {
        upstream.updateConfig(newConfig, servers);
      }
      return upstream;
    });
  }

  private void checkServersForAllUpstreamsInCurrentDcExist(Collection<String> upstreams, boolean throwValidation) {
    var upstreamsNotPresentInCurrentDC = upstreams.stream()
      .filter(upstream -> serverStore.getServers(upstream).stream().noneMatch(this::isInCurrentDc))
      .collect(Collectors.toSet());
    if (!upstreamsNotPresentInCurrentDC.isEmpty()) {
      if (throwValidation) {
        throw new IllegalStateException("There's no instances in DC " + datacenter + " for services: " + upstreamsNotPresentInCurrentDC
          + ". If it is intentional config use " + IGNORE_NO_SERVERS_IN_CURRENT_DC_KEY + " property to disable this check"
        );
      } else {
        LOGGER.debug("There's no instances in DC {} for services: {}. If it is intentional config use {} property to disable this check",
                     datacenter, upstreamsNotPresentInCurrentDC, IGNORE_NO_SERVERS_IN_CURRENT_DC_KEY);
      }
    }
  }

  private boolean isInCurrentDc(Server server) {
    return datacenter == null || server.getDatacenter() != null && server.getDatacenter().equals(datacenter);
  }

  private void checkServersForAllUpstreamsExist(Collection<String> upstreams, boolean throwValidation) {
    var emptyUpstreams = upstreams.stream()
      .filter(upstream -> serverStore.getServers(upstream).isEmpty())
      .collect(Collectors.toSet());
    if (!emptyUpstreams.isEmpty()) {
      if (throwValidation) {
        throw new IllegalStateException("There's no instances for services: " + emptyUpstreams
          + ". If it is intentional config use " + SYNC_UPDATE_KEY + " property to disable this check");
      } else {
        LOGGER.debug("There's no instances for services: {}", emptyUpstreams);
      }
    }
  }

  private void checkAllUpstreamConfigsExist(Collection<String> upstreams, boolean throwValidation) {
    var absentConfigs = upstreams.stream()
      .filter(upstream -> configStore.getUpstreamConfig(upstream) == null)
      .collect(Collectors.toSet());
    if (!absentConfigs.isEmpty()) {
      if (throwValidation) {
        throw new IllegalStateException("No valid configs found for services: " + absentConfigs);
      } else {
        LOGGER.debug("No valid configs found for services: {}", absentConfigs);
      }
    }
  }

  private Upstream createUpstream(Upstream.UpstreamKey key, Map<String, UpstreamConfig>  config, List<Server> servers) {
    return new Upstream(key, config, servers, datacenter, allowCrossDCRequests, true);
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

  public static class ValidationSettings {
    private double allowedDegradationPart;
    private boolean ignoreNoServersInCurrentDC;
    private boolean failOnEmptyUpstreams;

    public ValidationSettings setAllowedDegradationPart(double allowedDegradationPart) {
      this.allowedDegradationPart = allowedDegradationPart;
      return this;
    }

    public ValidationSettings setIgnoreNoServersInCurrentDC(boolean ignoreNoServersInCurrentDC) {
      this.ignoreNoServersInCurrentDC = ignoreNoServersInCurrentDC;
      return this;
    }

    public ValidationSettings setFailOnEmptyUpstreams(boolean failOnEmptyUpstreams) {
      this.failOnEmptyUpstreams = failOnEmptyUpstreams;
      return this;
    }
  }
}
