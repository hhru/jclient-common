package ru.hh.jclient.common.metric;

import java.util.function.Supplier;

public interface MetricProvider {

  Supplier<Integer> applicationThreadPoolSizeProvider();
  Supplier<Integer> applicationThreadPoolActiveTaskSizeProvider();
  Supplier<Integer> applicationThreadPoolQueueSizeProvider();

  Supplier<Integer> nettyBossThreadPoolSizeProvider();
  Supplier<Integer> nettyBossThreadPoolActiveTaskSizeProvider();
  Supplier<Integer> nettyBossThreadPoolQueueSizeProvider();

  Supplier<Integer> nettyChannelPoolSizeProvider();

  boolean containsApplicationThreadPoolMetrics();
  boolean containsNettyBossThreadPoolMetrics();
  boolean containsNettyChannelMetrics();
}
