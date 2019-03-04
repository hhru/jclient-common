package ru.hh.jclient.common.metrics;

import java.util.function.Supplier;

public interface MetricsProvider {
  Supplier<Long> totalConnectionCount();
  Supplier<Long> totalActiveConnectionCount();
  Supplier<Long> totalIdleConnectionCount();

  Supplier<Long> usedDirectMemory();
  Supplier<Long> usedHeapMemory();

  Supplier<Long> numActiveTinyAllocations();
  Supplier<Long> numActiveSmallAllocations();
  Supplier<Long> numActiveNormalAllocations();
  Supplier<Long> numActiveHugeAllocations();
}
