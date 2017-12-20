package ru.hh.jclient.common.metrics;

import ru.hh.jclient.common.MetricProvider;
import ru.hh.metrics.StatsDSender;
import ru.hh.metrics.Tag;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class StatsDMetricConnector implements Consumer<MetricProvider> {

  private final StatsDSender statsDSender;
  private final String name;

  public StatsDMetricConnector(StatsDSender statsDSender, String name) {
    this.statsDSender = statsDSender;
    this.name = name;
  }

  @Override
  public void accept(MetricProvider metricProvider) {
    if (metricProvider.containsThreadMetrics()) {
      Supplier<Long> threadPoolSizeSupplier = () -> Long.valueOf(metricProvider.threadPoolSizeProvider().get());
      statsDSender.sendMetricPeriodically("async.client.thread.pool.size", threadPoolSizeSupplier, new Tag("clientName", name));
      Supplier<Long> threadPoolActiveTaskCountSupplier = () -> Long.valueOf(metricProvider.threadPoolActiveTaskSizeProvider().get());
      statsDSender.sendMetricPeriodically("async.client.thread.pool.active.task.count", threadPoolActiveTaskCountSupplier,
          new Tag("clientName", name));
    }
  }
}
