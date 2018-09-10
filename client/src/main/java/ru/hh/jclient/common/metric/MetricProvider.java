package ru.hh.jclient.common.metric;

import java.util.function.Supplier;

public interface MetricProvider {
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
