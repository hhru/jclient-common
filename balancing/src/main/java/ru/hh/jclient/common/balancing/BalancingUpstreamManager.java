package ru.hh.jclient.common.balancing;

import com.google.common.annotations.VisibleForTesting;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.consul.UpstreamConfigService;
import ru.hh.jclient.consul.UpstreamService;
import ru.hh.jclient.consul.model.ApplicationConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BalancingUpstreamManager implements UpstreamManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(BalancingUpstreamManager.class);
  private static final int SCHEMA_SEPARATOR_LEN = 3;

  public static final String SCHEMA_SEPARATOR = "://";

  private final Map<String, UpstreamGroup> upstreams = new ConcurrentHashMap<>();
  private final Set<Monitoring> monitoring;
  private final String datacenter;
  private final boolean allowCrossDCRequests;
  private final UpstreamConfigService upstreamConfigService;
  private final UpstreamService upstreamService;
  private final Map<String, Integer> allowedUpstreamCapacities;

  public BalancingUpstreamManager(Collection<String> upstreamsList,
                                  Set<Monitoring> monitoring,
                                  String datacenter,
                                  boolean allowCrossDCRequests,
                                  UpstreamConfigService upstreamConfigService,
                                  UpstreamService upstreamService,
                                  double allowedDegradationPath) {
    this.monitoring = requireNonNull(monitoring, "monitorings must not be null");
    this.datacenter = datacenter == null ? null : datacenter.toLowerCase();
    this.allowCrossDCRequests = allowCrossDCRequests;
    this.upstreamService = upstreamService;
    this.upstreamConfigService = upstreamConfigService;

    requireNonNull(upstreamsList, "upstreamsList must not be null");
    upstreamsList.forEach(this::updateUpstream);
    allowedUpstreamCapacities = upstreams.entrySet().stream()
      .collect(toMap(Map.Entry::getKey, e -> (int)Math.ceil(e.getValue().getMinServerSize() * (1 - allowedDegradationPath))));
    upstreamConfigService.setupListener(this::updateUpstream);
    upstreamService.setupListener(this::updateUpstream);
  }

  public void updateUpstream(@Nonnull String upstreamName) {
    var upstreamKey = Upstream.UpstreamKey.ofComplexName(upstreamName);

    ApplicationConfig upstreamConfig = upstreamConfigService.getUpstreamConfig(upstreamKey.getServiceName());
    var newConfig = UpstreamConfig.fromApplicationConfig(upstreamConfig, UpstreamConfig.DEFAULT, upstreamKey.getProfileName());
    List<Server> servers = upstreamService.getServers(upstreamName);
    boolean unsafeUpdate = ofNullable(allowedUpstreamCapacities).map(capacities -> capacities.get(upstreamKey.getServiceName()))
      .map(allowedUpstreamCapacity -> servers.size() < allowedUpstreamCapacity)
      .orElse(Boolean.FALSE);
    if (unsafeUpdate) {
      monitoring.forEach(m -> m.countUpdateIgnore(upstreamName, datacenter));
      LOGGER.warn("Ignoring update which contains {} servers, for upstream {} allowed minimum is {}",
        LOGGER.isDebugEnabled() ? servers : servers.size(),
        upstreamKey.getServiceName(),
        allowedUpstreamCapacities.get(upstreamKey.getServiceName())
      );
      return;
    }
    upstreams.compute(upstreamKey.getServiceName(), (serviceName, existingGroup) -> {
      if (existingGroup == null) {
        return new UpstreamGroup(serviceName, upstreamKey.getProfileName(), createUpstream(upstreamKey, newConfig, servers));
      }
      return existingGroup.addOrUpdate(upstreamKey.getProfileName(), newConfig, servers,
          (profileName, config) -> createUpstream(upstreamKey, newConfig, servers)
      );
    });
  }

  private Upstream createUpstream(Upstream.UpstreamKey key, UpstreamConfig config, List<Server> servers) {
    return new Upstream(key, config, servers, datacenter, allowCrossDCRequests, true);
  }

  @Override
  public Upstream getUpstream(String serviceName, @Nullable String profile) {
    return ofNullable(upstreams.get(getNameWithoutScheme(serviceName)))
        .map(group -> group.getUpstreamOrDefault(profile)).orElse(null);
  }

  @Override
  public Set<Monitoring> getMonitoring() {
    return Set.copyOf(monitoring);
  }

  static String getNameWithoutScheme(String host) {
    int beginIndex = host.indexOf(SCHEMA_SEPARATOR) + SCHEMA_SEPARATOR_LEN;
    return beginIndex > 2 ? host.substring(beginIndex) : host;
  }

  @VisibleForTesting
  Map<String, UpstreamGroup> getUpstreams() {
    return upstreams;
  }
}
