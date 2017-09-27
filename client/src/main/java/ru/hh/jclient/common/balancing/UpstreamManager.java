package ru.hh.jclient.common.balancing;

import com.ning.http.client.uri.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UpstreamManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamManager.class);

  private final Map<String, Upstream> upstreams = new ConcurrentHashMap<>();
  private final ScheduledExecutorService periodicTasksExecutor;

  public UpstreamManager(Map<String, String> upstreamConfigs, int logStatsIntervalMs) {
    periodicTasksExecutor = Executors.newSingleThreadScheduledExecutor((Runnable r) -> {
      Thread thread = new Thread(r,"jclient_upstreams_manager");
      thread.setDaemon(true);
      return thread;
    });
    upstreamConfigs.forEach(this::updateUpstream);
    if (logStatsIntervalMs > 0) {
      periodicTasksExecutor.scheduleWithFixedDelay(this::logStats, logStatsIntervalMs, logStatsIntervalMs, TimeUnit.MILLISECONDS);
    }
  }

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
      upstreams.put(name, new Upstream(name, upstreamConfig, periodicTasksExecutor));
    }
  }

  public void logStats() {
    upstreams.forEach((host, upstream) -> {
      LOGGER.info("upstream {} servers: {}", host, upstream.getStats());
      upstream.resetStats();
    });
  }

  public Upstream getUpstream(String host) {
    return upstreams.get(getNameWithoutScheme(host));
  }

  public Map<String, Upstream> getUpstreams() {
    return Collections.unmodifiableMap(upstreams);
  }

  private static String getNameWithoutScheme(String host) {
    if (host.startsWith("http")) {
      String scheme = Uri.create(host).getScheme() + "://";
      host = host.substring(scheme.length());
    }
    return host;
  }
}
