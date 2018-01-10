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
  private static final String NAME_KEY = "name";

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
    if (metricProvider == null || !metricProvider.containsThreadMetrics()) {
      log.info("Metric provider contains no metrics, won't schedule anything");
      return;
    }
    future = scheduler.scheduleAtFixedRate(() -> {
      statsDClient.gauge("async.client.thread.pool.size", metricProvider.threadPoolSizeProvider().get(), nameTag);
      statsDClient.gauge("async.client.thread.pool.active.task.count", metricProvider.threadPoolActiveTaskSizeProvider().get(), nameTag);
    }, 0, sendIntervalAmount, sendIntervalUnit);
    log.info("Successfully scheduled metrics sending");

  }
}
