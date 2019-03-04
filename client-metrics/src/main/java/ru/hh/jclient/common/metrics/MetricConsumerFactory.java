package ru.hh.jclient.common.metrics;

import com.timgroup.statsd.StatsDClient;

import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

public class MetricConsumerFactory {
  private MetricConsumerFactory() {}

  private static MetricsConsumer NOOP_METRIC_CONSUMER = metricProvider -> {};

  public static MetricsConsumer buildMetricConsumerFromProperties(Properties properties, String name, StatsDClient statsDClient,
                                                                  ScheduledExecutorService scheduler) {
    if (!ofNullable(properties.getProperty("enabled")).map(Boolean::parseBoolean).orElse(Boolean.FALSE)) {
      return NOOP_METRIC_CONSUMER;
    }

    return ofNullable(properties.getProperty("sendIntervalMs")).map(Integer::parseInt)
      .<MetricsConsumer>map(sendIntervalMs -> new StatsDMetricsConsumer(name, statsDClient, scheduler, sendIntervalMs, TimeUnit.MILLISECONDS))
      .orElse(NOOP_METRIC_CONSUMER);
  }
}
