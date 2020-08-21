package ru.hh.jclient.common.balancing;

import ru.hh.jclient.consul.ConsulConfigService;
import ru.hh.jclient.consul.ConsulUpstreamService;
import com.google.common.annotations.VisibleForTesting;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.Monitoring;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

public class BalancingUpstreamManager extends UpstreamManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(BalancingUpstreamManager.class);
  static final String SCHEMA_SEPARATOR = "://";
  private static final int SCHEMA_SEPARATOR_LEN = 3;

  private final Map<String, UpstreamGroup> upstreams = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduledExecutor;
  private final Set<Monitoring> monitoring;
  private final String datacenter;
  private final boolean allowCrossDCRequests;
  private final ConsulConfigService consulConfigService;
  private final ConsulUpstreamService consulUpstreamService;

  public BalancingUpstreamManager(ScheduledExecutorService scheduledExecutor, Set<Monitoring> monitoring,
                                  String datacenter,
                                  boolean allowCrossDCRequests, ConsulConfigService consulConfigService,
                                  ConsulUpstreamService consulUpstreamService) {
    this(Map.of(), scheduledExecutor, monitoring, datacenter, allowCrossDCRequests, consulConfigService, consulUpstreamService);
  }

  public BalancingUpstreamManager(Map<String, String> upstreamConfigs,
                                  ScheduledExecutorService scheduledExecutor,
                                  Set<Monitoring> monitoring,
                                  String datacenter,
                                  boolean allowCrossDCRequests, ConsulConfigService consulConfigService
  , ConsulUpstreamService consulUpstreamService) {
    this.scheduledExecutor = requireNonNull(scheduledExecutor, "scheduledExecutor must not be null");
    this.monitoring = requireNonNull(monitoring, "monitorings must not be null");
    this.datacenter = datacenter;
    this.allowCrossDCRequests = allowCrossDCRequests;
    this.consulUpstreamService = consulUpstreamService;
    this.consulConfigService = consulConfigService;

    requireNonNull(upstreamConfigs, "upstreamConfigs must not be null");
    upstreamConfigs.forEach(this::updateUpstream);
  }

  @Override
  public void updateUpstream(@Nonnull String upstreamName, String configString) {
    var upstreamKey = Upstream.UpstreamKey.ofComplexName(upstreamName);
    if (configString == null) {
      LOGGER.info("removing upstream: {}", upstreamName);
      upstreams.computeIfPresent(upstreamKey.getServiceName(), (key, upstreamGroup) -> {
        upstreamGroup.remove(upstreamKey.getProfileName());
        if (upstreamGroup.isEmpty()) {
          return null;
        }
        return upstreamGroup;
      });
      return;
    }
    Map<String, Object> upstreamConfig = consulConfigService.getUpstreamConfig(upstreamKey.getServiceName(), upstreamKey.getProfileName());

    //todo host
    var newConfig = UpstreamConfig.fromMap(upstreamKey.getServiceName(), upstreamKey.getProfileName(), "default", upstreamConfig);
    upstreams.compute(upstreamKey.getServiceName(), (serviceName, existingGroup) -> {
      if (existingGroup == null) {
        return new UpstreamGroup(serviceName, upstreamKey.getProfileName(), createUpstream(upstreamKey, newConfig));
      }
      return existingGroup.addOrUpdate(upstreamKey.getProfileName(), newConfig,
          (profileName, config) -> createUpstream(upstreamKey, newConfig)
      );
    });
  }

  private Upstream createUpstream(Upstream.UpstreamKey key, UpstreamConfig config) {
    return new Upstream(key, config, scheduledExecutor, datacenter, allowCrossDCRequests, true);
  }

  @Override
  public Upstream getUpstream(String serviceName, @Nullable String profile) {
    return ofNullable(upstreams.get(getNameWithoutScheme(serviceName)))
        .map(group -> group.getUpstreamOrDefault(profile)).orElse(null);
  }

  @Override
  public ConsulUpstreamService getConsulUpstreamService() {
    return consulUpstreamService;
  }

  @Override
  public Set<Monitoring> getMonitoring() {
    return Set.copyOf(monitoring);
  }

  static String getNameWithoutScheme(String host) {
    int beginIndex = host.indexOf(SCHEMA_SEPARATOR) + SCHEMA_SEPARATOR_LEN;
    return beginIndex > 2 ? host.substring(beginIndex) : host;
  }

  @Override
  @VisibleForTesting
  Map<String, UpstreamGroup> getUpstreams() {
    return upstreams;
  }
}
