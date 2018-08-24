package ru.hh.jclient.common;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufAllocatorMetric;
import io.netty.buffer.ByteBufAllocatorMetricProvider;
import org.asynchttpclient.AsyncHttpClient;
import ru.hh.jclient.common.metric.MetricProvider;

import java.util.Optional;
import java.util.function.Supplier;

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
        return () -> getByteBufferMetrics(clientBuilder.getHttp()).map(ByteBufAllocatorMetric::usedDirectMemory).orElse(0L);
      }

      @Override
      public Supplier<Long> usedHeapMemory() {
        return () -> getByteBufferMetrics(clientBuilder.getHttp()).map(ByteBufAllocatorMetric::usedHeapMemory).orElse(0L);
      }
    };
  }

  private static Optional<ByteBufAllocatorMetric> getByteBufferMetrics(AsyncHttpClient asyncHttpClient) {
    ByteBufAllocator allocator = Optional.ofNullable(asyncHttpClient.getConfig().getAllocator()).orElse(ByteBufAllocator.DEFAULT);
    if (allocator instanceof ByteBufAllocatorMetricProvider) {
      ByteBufAllocatorMetricProvider metricProvider = (ByteBufAllocatorMetricProvider) allocator;
      return Optional.ofNullable(metricProvider.metric());
    }
    return Optional.empty();
  }
}
