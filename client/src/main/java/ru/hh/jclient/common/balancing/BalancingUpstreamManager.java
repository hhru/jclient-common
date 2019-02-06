package ru.hh.jclient.common.balancing;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.UpstreamManager;
import ru.hh.jclient.common.Uri;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

public final class BalancingUpstreamManager extends UpstreamManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalancingUpstreamManager.class);

  private final Map<String, Upstream> upstreams = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduledExecutor;
  private final Set<Monitoring> monitoring;
  private final String datacenter;
  private final boolean allowCrossDCRequests;

  public BalancingUpstreamManager(ScheduledExecutorService scheduledExecutor, Set<Monitoring> monitoring, String datacenter, boolean allowCrossDCRequests) {
    this(emptyMap(), scheduledExecutor, monitoring, datacenter, allowCrossDCRequests);
  }

  public BalancingUpstreamManager(Map<String, String> upstreamConfigs,
                                  ScheduledExecutorService scheduledExecutor,
                                  Set<Monitoring> monitoring,
                                  String datacenter,
                                  boolean allowCrossDCRequests) {
    this.scheduledExecutor = requireNonNull(scheduledExecutor, "scheduledExecutor must not be null");
    this.monitoring = requireNonNull(monitoring, "monitorings must not be null");
    this.datacenter = datacenter;
    this.allowCrossDCRequests = allowCrossDCRequests;

    requireNonNull(upstreamConfigs, "upstreamConfigs must not be null");
    upstreamConfigs.forEach(this::updateUpstream);
  }

  @Override
  public void updateUpstream(String name, String configString) {
    if (configString == null) {
      LOGGER.info("removing upstream: {}", name);
      upstreams.remove(name);
      return;
    }
    UpstreamConfig upstreamConfig = UpstreamConfig.parse(configString);
    Upstream upstream = upstreams.get(name);
    if (upstream != null) {
      LOGGER.info("updating upstream: {} {}", name, upstreamConfig);
      upstream.updateConfig(upstreamConfig);
    } else {
      LOGGER.info("adding upstream: {} {}", name, upstreamConfig);
      upstreams.put(name, new Upstream(name, upstreamConfig, scheduledExecutor, datacenter, allowCrossDCRequests));
    }
  }

  @Override
  public Upstream getUpstream(String host) {
    return upstreams.get(getNameWithoutScheme(host));
  }

  @Override
  public Map<String, Upstream> getUpstreams() {
    return Collections.unmodifiableMap(upstreams);
  }

  @Override
  public Set<Monitoring> getMonitoring() {
    return monitoring;
  }

  private static String getNameWithoutScheme(String host) {
    if (host.startsWith("http")) {
      String scheme = Uri.create(host).getScheme() + "://";
      host = host.substring(scheme.length());
    }
    return host;
  }
}
