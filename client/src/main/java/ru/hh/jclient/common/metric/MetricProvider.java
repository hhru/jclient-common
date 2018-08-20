package ru.hh.jclient.common.metric;

import java.util.function.Supplier;

public interface MetricProvider {
  Supplier<Long> totalConnectionCount();
  Supplier<Long> totalActiveConnectionCount();
  Supplier<Long> totalIdleConnectionCount();
}
