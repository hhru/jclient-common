package ru.hh.jclient.common;

import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpProviderConfig;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;
import ru.hh.jclient.common.metric.MetricProvider;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

public final class MetricProviderFactory {

  private MetricProviderFactory() {
  }

  public static MetricProvider from(HttpClientBuilder clientBuilder) {
    AsyncHttpClientConfig config = clientBuilder.getHttp().getConfig();
    boolean applicationExecutorServiceMeasurable = config.executorService() instanceof ThreadPoolExecutor;

    AsyncHttpProviderConfig<?, ?> asyncHttpProviderConfig = config.getAsyncHttpProviderConfig();
    boolean nettyBossExecutorServiceMeasurable = asyncHttpProviderConfig instanceof NettyAsyncHttpProviderConfig
        && extract(asyncHttpProviderConfig) instanceof ThreadPoolExecutor;

    ExecutorService applicationExecutorService = config.executorService();
    ExecutorService nettyBossExecutorService = extract(asyncHttpProviderConfig);

    return new MetricProvider() {
      @Override
      public Supplier<Integer> applicationThreadPoolSizeProvider() {
        return ((ThreadPoolExecutor) applicationExecutorService)::getPoolSize;
      }

      @Override
      public Supplier<Integer> applicationThreadPoolActiveTaskSizeProvider() {
        return ((ThreadPoolExecutor) applicationExecutorService)::getActiveCount;
      }

      @Override
      public Supplier<Integer> applicationThreadPoolQueueSizeProvider() {
        return () -> Optional.ofNullable(((ThreadPoolExecutor) applicationExecutorService).getQueue()).map(Queue::size).orElse(0);
      }

      @Override
      public Supplier<Integer> nettyBossThreadPoolSizeProvider() {
        return ((ThreadPoolExecutor) nettyBossExecutorService)::getPoolSize;
      }

      @Override
      public Supplier<Integer> nettyBossThreadPoolActiveTaskSizeProvider() {
        return ((ThreadPoolExecutor) nettyBossExecutorService)::getActiveCount;
      }

      @Override
      public Supplier<Integer> nettyBossthreadPoolQueueSizeProvider() {
        return () -> Optional.ofNullable(((ThreadPoolExecutor) nettyBossExecutorService).getQueue()).map(Queue::size).orElse(0);
      }

      @Override
      public boolean containsApplicationThreadPoolMetrics() {
        return applicationExecutorServiceMeasurable;
      }

      @Override
      public boolean containsNettyBossThreadPoolMetrics() {
        return nettyBossExecutorServiceMeasurable;
      }
    };
  }

  private static ExecutorService extract(AsyncHttpProviderConfig<?, ?> asyncHttpProviderConfig) {
    return ((NettyAsyncHttpProviderConfig) asyncHttpProviderConfig).getBossExecutorService();
  }
}
