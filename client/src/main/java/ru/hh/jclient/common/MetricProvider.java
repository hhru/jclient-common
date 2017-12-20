package ru.hh.jclient.common;

import java.util.function.Supplier;

public interface MetricProvider {

  Supplier<Integer> threadPoolSizeProvider();
  Supplier<Integer> threadPoolActiveTaskSizeProvider();
  boolean containsThreadMetrics();

  MetricProvider EMPTY = new MetricProvider() {
    @Override
    public Supplier<Integer> threadPoolSizeProvider() {
      return null;
    }

    @Override
    public Supplier<Integer> threadPoolActiveTaskSizeProvider() {
      return null;
    }

    @Override
    public boolean containsThreadMetrics() {
      return false;
    }
  };
}
