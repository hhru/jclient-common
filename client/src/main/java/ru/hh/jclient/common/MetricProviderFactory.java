package ru.hh.jclient.common;

import com.ning.http.client.AsyncHttpClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

public final class MetricProviderFactory {

  public static MetricProvider from(AsyncHttpClient asyncHttpClient, HttpClientConfig config) {
    ExecutorService executorService = asyncHttpClient.getConfig().executorService();
    if (!(executorService instanceof ThreadPoolExecutor)) {
      return MetricProvider.EMPTY;
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
