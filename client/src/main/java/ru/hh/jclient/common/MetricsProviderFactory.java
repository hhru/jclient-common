package ru.hh.jclient.common;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufAllocatorMetric;
import io.netty.buffer.PoolArenaMetric;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocatorMetric;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.function.Supplier;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import ru.hh.jclient.common.metrics.MetricsProvider;

public final class MetricsProviderFactory {

  private MetricsProviderFactory() {
  }

  public static MetricsProvider from(AsyncHttpClient httpClient) {
    return new MetricsProvider() {
      @Override
      public Supplier<Long> totalConnectionCount() {
        return () -> httpClient.getClientStats().getTotalConnectionCount();
      }

      @Override
      public Supplier<Long> totalActiveConnectionCount() {
        return () -> httpClient.getClientStats().getTotalActiveConnectionCount();
      }

      @Override
      public Supplier<Long> totalIdleConnectionCount() {
        return () -> httpClient.getClientStats().getTotalIdleConnectionCount();
      }

      @Override
      public Supplier<Long> usedDirectMemory() {
        return () -> getAllocatorMetrics(httpClient).map(ByteBufAllocatorMetric::usedDirectMemory).orElse(0L);
      }

      @Override
      public Supplier<Long> usedHeapMemory() {
        return () -> getAllocatorMetrics(httpClient).map(ByteBufAllocatorMetric::usedHeapMemory).orElse(0L);
      }

      @Override
      public Supplier<Long> numActiveTinyAllocations() {
        return () -> getAllocatorMetrics(httpClient)
            .map(a -> a.directArenas().stream())
            .map(a -> a.mapToLong(PoolArenaMetric::numActiveTinyAllocations).sum())
            .orElse(0L);
      }

      @Override
      public Supplier<Long> numActiveSmallAllocations() {
        return () -> getAllocatorMetrics(httpClient)
            .map(a -> a.directArenas().stream())
            .map(a -> a.mapToLong(PoolArenaMetric::numActiveSmallAllocations).sum())
            .orElse(0L);
      }

      @Override
      public Supplier<Long> numActiveNormalAllocations() {
        return () -> getAllocatorMetrics(httpClient)
            .map(a -> a.directArenas().stream())
            .map(a -> a.mapToLong(PoolArenaMetric::numActiveNormalAllocations).sum())
            .orElse(0L);
      }

      @Override
      public Supplier<Long> numActiveHugeAllocations() {
        return () -> getAllocatorMetrics(httpClient)
            .map(a -> a.directArenas().stream())
            .map(a -> a.mapToLong(PoolArenaMetric::numActiveHugeAllocations).sum())
            .orElse(0L);
      }

      @Override
      public Supplier<Long> epollTotalPendingTasks() {
        return () -> {
          if (httpClient instanceof DefaultAsyncHttpClient defaultAsyncHttpClient) {
            var eventLoopGroup = defaultAsyncHttpClient.getEventLoopGroup();
            return eventLoopGroup instanceof EpollEventLoopGroup ? calcPendingTasksCount(eventLoopGroup) : 0L;
          }
          return 0L;
        };
      }

      @Override
      public Supplier<Long> nioTotalPendingTasks() {
        return () -> {
          if (httpClient instanceof DefaultAsyncHttpClient defaultAsyncHttpClient) {
            var eventLoopGroup = defaultAsyncHttpClient.getEventLoopGroup();
            return eventLoopGroup instanceof NioEventLoopGroup ? calcPendingTasksCount(eventLoopGroup) : 0L;
          }
          return 0L;
        };
      }

      @Override
      public Supplier<Long> epollPendingThreads() {
        return () -> {
          if (httpClient instanceof DefaultAsyncHttpClient defaultAsyncHttpClient) {
            var eventLoopGroup = defaultAsyncHttpClient.getEventLoopGroup();
            return eventLoopGroup instanceof EpollEventLoopGroup ? calcPendingThreadsCount(eventLoopGroup) : 0L;
          }
          return 0L;
        };
      }

      @Override
      public Supplier<Long> nioPendingThreads() {
        return () -> {
          if (httpClient instanceof DefaultAsyncHttpClient defaultAsyncHttpClient) {
            var eventLoopGroup = defaultAsyncHttpClient.getEventLoopGroup();
            return eventLoopGroup instanceof NioEventLoopGroup ? calcPendingThreadsCount(eventLoopGroup) : 0L;
          }
          return 0L;
        };
      }

      @Override
      public Supplier<Integer> epollMaxThreads() {
        return () -> {
          if (httpClient instanceof DefaultAsyncHttpClient defaultAsyncHttpClient) {
            var eventLoopGroup = defaultAsyncHttpClient.getEventLoopGroup();
            if (eventLoopGroup instanceof EpollEventLoopGroup epollEventLoopGroup) {
              return epollEventLoopGroup.executorCount();
            }
          }
          return 0;
        };
      }

      @Override
      public Supplier<Integer> nioMaxThreads() {
        return () -> {
          if (httpClient instanceof DefaultAsyncHttpClient defaultAsyncHttpClient) {
            var eventLoopGroup = defaultAsyncHttpClient.getEventLoopGroup();
            if (eventLoopGroup instanceof NioEventLoopGroup nioEventLoopGroup) {
              return nioEventLoopGroup.executorCount();
            }
          }
          return 0;
        };
      }
    };
  }

  private static long calcPendingTasksCount(EventLoopGroup eventLoopGroup) {
    long pendingTasksTotalCount = 0;
    for (var eventExecutor : eventLoopGroup) {
      if (eventExecutor instanceof SingleThreadEventLoop singleThreadEventLoop) {
        int pendingTasksCount = singleThreadEventLoop.pendingTasks();
        pendingTasksTotalCount += pendingTasksCount;
      }
    }
    return pendingTasksTotalCount;
  }

  private static long calcPendingThreadsCount(EventLoopGroup eventLoopGroup) {
    long pendingThreadsCount = 0;
    for (var eventExecutor : eventLoopGroup) {
      if (eventExecutor instanceof SingleThreadEventLoop singleThreadEventLoop) {
        int pendingTasksCount = singleThreadEventLoop.pendingTasks();
        pendingThreadsCount += pendingTasksCount > 0 ? 1 : 0;
      }
    }
    return pendingThreadsCount;
  }

  private static Optional<PooledByteBufAllocatorMetric> getAllocatorMetrics(AsyncHttpClient asyncHttpClient) {
    ByteBufAllocator allocator = ofNullable(asyncHttpClient.getConfig().getAllocator()).orElse(ByteBufAllocator.DEFAULT);
    if (allocator instanceof PooledByteBufAllocator) {
      return Optional.ofNullable(((PooledByteBufAllocator) allocator).metric());
    }
    return Optional.empty();
  }
}
