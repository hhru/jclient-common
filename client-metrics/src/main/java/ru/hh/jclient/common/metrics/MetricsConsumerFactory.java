package ru.hh.jclient.common.metrics;

import com.timgroup.statsd.StatsDClient;

import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

public class MetricsConsumerFactory {
  private MetricsConsumerFactory() {}

  private static final MetricsConsumer NOOP_METRICS_CONSUMER = metricsProvider -> {};

  public static MetricsConsumer buildMetricsConsumer(Properties properties, String name, StatsDClient statsDClient,
                                                     ScheduledExecutorService scheduler) {
    if (!ofNullable(properties.getProperty("enabled")).map(Boolean::parseBoolean).orElse(Boolean.FALSE)) {
      return NOOP_METRICS_CONSUMER;
    }

    return ofNullable(properties.getProperty("sendIntervalMs")).map(Integer::parseInt)
      .<MetricsConsumer>map(sendIntervalMs -> new StatsDMetricsConsumer(name, statsDClient, scheduler, sendIntervalMs, TimeUnit.MILLISECONDS))
      .orElse(NOOP_METRICS_CONSUMER);
  }
}
