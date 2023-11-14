package ru.hh.jclient.common;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufAllocatorMetric;
import io.netty.buffer.PoolArenaMetric;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocatorMetric;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.function.Supplier;
import org.asynchttpclient.AsyncHttpClient;
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
    };
  }

  private static Optional<PooledByteBufAllocatorMetric> getAllocatorMetrics(AsyncHttpClient asyncHttpClient) {
    ByteBufAllocator allocator = ofNullable(asyncHttpClient.getConfig().getAllocator()).orElse(ByteBufAllocator.DEFAULT);
    if (allocator instanceof PooledByteBufAllocator) {
      return Optional.ofNullable(((PooledByteBufAllocator) allocator).metric());
    }
    return Optional.empty();
  }
}
