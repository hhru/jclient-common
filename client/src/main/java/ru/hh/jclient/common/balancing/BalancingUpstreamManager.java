package ru.hh.jclient.common.balancing;

import com.ning.http.client.uri.Uri;
import static java.util.Objects.requireNonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.UpstreamManager;
import ru.hh.jclient.common.Monitoring;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

public class BalancingUpstreamManager implements UpstreamManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalancingUpstreamManager.class);

  private final Map<String, Upstream> upstreams = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduledExecutor;
  private final Monitoring monitoring;

  public BalancingUpstreamManager(Map<String, String> upstreamConfigs, ScheduledExecutorService scheduledExecutor, Monitoring monitoring) {
    requireNonNull(upstreamConfigs, "upstreamConfigs must not be null");
    upstreamConfigs.forEach(this::updateUpstream);

    this.scheduledExecutor = requireNonNull(scheduledExecutor, "scheduledExecutor must not be null");
    this.monitoring = requireNonNull(monitoring, "monitoring must not be null");
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
      upstreams.put(name, new Upstream(name, upstreamConfig, scheduledExecutor));
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
  public Monitoring getMonitoring() {
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
