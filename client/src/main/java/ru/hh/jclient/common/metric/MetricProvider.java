package ru.hh.jclient.common.metric;

import java.util.function.Supplier;

public interface MetricProvider {

  Supplier<Integer> threadPoolSizeProvider();
  Supplier<Integer> threadPoolActiveTaskSizeProvider();
  boolean containsThreadMetrics();
}
