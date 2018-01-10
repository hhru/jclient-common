package ru.hh.jclient.common;

import ru.hh.jclient.common.metric.MetricProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

public final class MetricProviderFactory {

  public static MetricProvider from(HttpClientBuilder clientBuilder) {
    ExecutorService executorService = clientBuilder.getHttp().getConfig().executorService();
    if (!(executorService instanceof ThreadPoolExecutor)) {
      return null;
    }

    return new MetricProvider() {
      @Override
      public Supplier<Integer> threadPoolSizeProvider() {
        return ((ThreadPoolExecutor)executorService)::getPoolSize;
      }

      @Override
      public Supplier<Integer> threadPoolActiveTaskSizeProvider() {
        return ((ThreadPoolExecutor)executorService)::getActiveCount;
      }

      @Override
      public boolean containsThreadMetrics() {
        return true;
      }
    };
  }
}
