package ru.hh.jclient.common.metrics;

import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.metric.MetricConsumer;
import ru.hh.jclient.common.metric.MetricProvider;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class StatsDMetricConsumer implements MetricConsumer {

  private static final Logger log = LoggerFactory.getLogger(StatsDMetricConsumer.class);
  private static final String NAME_KEY = "clientName";

  private final String nameTag;
  private final StatsDClient statsDClient;
  private final ScheduledExecutorService scheduler;
  private final long sendIntervalAmount;
  private final TimeUnit sendIntervalUnit;

  private ScheduledFuture<?> future;

  public StatsDMetricConsumer(String name, StatsDClient statsDClient, ScheduledExecutorService scheduler,
                              long sendIntervalAmount, TimeUnit sendIntervalUnit) {
    this.nameTag = buildNameTag(name);
    this.statsDClient = statsDClient;
    this.scheduler = scheduler;
    this.sendIntervalAmount = sendIntervalAmount;
    this.sendIntervalUnit = sendIntervalUnit;
  }

  private static String buildNameTag(String name) {
    return NAME_KEY + "_is_" + name.replace('.', '-');
  }

  public void disconnect() {
    Optional.ofNullable(future).ifPresent(scheduledFuture -> scheduledFuture.cancel(false));
  }

  @Override
  public void accept(MetricProvider metricProvider) {
    if (metricProvider == null) {
      log.info("Metric provider contains no metrics, won't schedule anything");
      return;
    }
    future = scheduler.scheduleAtFixedRate(() -> {
      if (metricProvider.containsApplicationThreadPoolMetrics()) {
        statsDClient.gauge(getFullMetricName("async.client.application.thread.pool.size", nameTag),
            metricProvider.applicationThreadPoolSizeProvider().get());
        statsDClient.gauge(getFullMetricName("async.client.application.thread.pool.active.task.count", nameTag),
            metricProvider.applicationThreadPoolActiveTaskSizeProvider().get());
        statsDClient.gauge(getFullMetricName("async.client.application.thread.pool.queue.size", nameTag),
            metricProvider.applicationThreadPoolQueueSizeProvider().get());
      }
      if (metricProvider.containsNettyBossThreadPoolMetrics()) {
        statsDClient.gauge(getFullMetricName("async.client.netty.boss.thread.pool.size", nameTag),
            metricProvider.nettyBossThreadPoolSizeProvider().get());
        statsDClient.gauge(getFullMetricName("async.client.netty.boss.thread.pool.active.task.count", nameTag),
            metricProvider.nettyBossThreadPoolActiveTaskSizeProvider().get());
        statsDClient.gauge(getFullMetricName("async.client.netty.boss.thread.pool.queue.size", nameTag),
            metricProvider.nettyBossthreadPoolQueueSizeProvider().get());
      }
    }, 0, sendIntervalAmount, sendIntervalUnit);
    log.info("Successfully scheduled metrics sending");

  }

  private static String getFullMetricName(String metricName, String... tags) {
    if (tags == null) {
      return metricName;
    }
    return String.join(".", metricName, String.join(".", tags));
  }
}
