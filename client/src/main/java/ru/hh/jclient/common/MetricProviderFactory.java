package ru.hh.jclient.common;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufAllocatorMetric;
import io.netty.buffer.PoolArenaMetric;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocatorMetric;
import org.asynchttpclient.AsyncHttpClient;
import ru.hh.jclient.common.metric.MetricProvider;

import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

public final class MetricProviderFactory {

  private MetricProviderFactory() {
  }

  public static MetricProvider from(HttpClientBuilder clientBuilder) {
    return new MetricProvider() {
      @Override
      public Supplier<Long> totalConnectionCount() {
        return () -> clientBuilder.getHttp().getClientStats().getTotalConnectionCount();
      }

      @Override
      public Supplier<Long> totalActiveConnectionCount() {
        return () -> clientBuilder.getHttp().getClientStats().getTotalActiveConnectionCount();
      }

      @Override
      public Supplier<Long> totalIdleConnectionCount() {
        return () -> clientBuilder.getHttp().getClientStats().getTotalIdleConnectionCount();
      }

      @Override
      public Supplier<Long> usedDirectMemory() {
        return () -> getAllocatorMetrics(clientBuilder.getHttp()).map(ByteBufAllocatorMetric::usedDirectMemory).orElse(0L);
      }

      @Override
      public Supplier<Long> usedHeapMemory() {
        return () -> getAllocatorMetrics(clientBuilder.getHttp()).map(ByteBufAllocatorMetric::usedHeapMemory).orElse(0L);
      }

      @Override
      public Supplier<Long> numActiveTinyAllocations() {
        return () -> getAllocatorMetrics(clientBuilder.getHttp()).map(a -> a.directArenas().stream())
          .map(a -> a.mapToLong(PoolArenaMetric::numActiveTinyAllocations).sum()).orElse(0L);
      }

      @Override
      public Supplier<Long> numActiveSmallAllocations() {
        return () -> getAllocatorMetrics(clientBuilder.getHttp()).map(a -> a.directArenas().stream())
          .map(a -> a.mapToLong(PoolArenaMetric::numActiveSmallAllocations).sum()).orElse(0L);
      }

      @Override
      public Supplier<Long> numActiveNormalAllocations() {
        return () -> getAllocatorMetrics(clientBuilder.getHttp()).map(a -> a.directArenas().stream())
          .map(a -> a.mapToLong(PoolArenaMetric::numActiveNormalAllocations).sum()).orElse(0L);
      }

      @Override
      public Supplier<Long> numActiveHugeAllocations() {
        return () -> getAllocatorMetrics(clientBuilder.getHttp()).map(a -> a.directArenas().stream())
          .map(a -> a.mapToLong(PoolArenaMetric::numActiveHugeAllocations).sum()).orElse(0L);
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
