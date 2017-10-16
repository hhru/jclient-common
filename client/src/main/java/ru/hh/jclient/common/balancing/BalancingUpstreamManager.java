package ru.hh.jclient.common.balancing;

import com.ning.http.client.uri.Uri;
import static java.util.Objects.requireNonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.UpstreamManager;
import ru.hh.metrics.StatsDSender;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BalancingUpstreamManager implements UpstreamManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalancingUpstreamManager.class);

  private final Map<String, Upstream> upstreams = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduledExecutor;
  private final UpstreamMonitoring upstreamMonitoring;

  public BalancingUpstreamManager(Map<String, String> upstreamConfigs,
                                  ScheduledExecutorService scheduledExecutor,
                                  StatsDSender statsDSender,
                                  String serviceName,
                                  int logStatsIntervalMs) {
    requireNonNull(upstreamConfigs, "upstreamConfigs must not be null");
    upstreamConfigs.forEach(this::updateUpstream);

    this.scheduledExecutor = requireNonNull(scheduledExecutor, "scheduledExecutor must not be null");
    if (logStatsIntervalMs > 0) {
      scheduledExecutor.scheduleWithFixedDelay(this::logStats, logStatsIntervalMs, logStatsIntervalMs, TimeUnit.MILLISECONDS);
    }
    upstreamMonitoring = new UpstreamMonitoring(statsDSender, serviceName);
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
  public UpstreamMonitoring getMonitoring() {
    return upstreamMonitoring;
  }

  private void logStats() {
    upstreams.forEach((host, upstream) -> {
      LOGGER.info("upstream {} servers: {}", host, upstream.getStats());
      upstream.resetStats();
    });
  }

  private static String getNameWithoutScheme(String host) {
    if (host.startsWith("http")) {
      String scheme = Uri.create(host).getScheme() + "://";
      host = host.substring(scheme.length());
    }
    return host;
  }
}
