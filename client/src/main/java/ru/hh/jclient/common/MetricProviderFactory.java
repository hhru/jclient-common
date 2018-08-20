package ru.hh.jclient.common;

import ru.hh.jclient.common.metric.MetricProvider;

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
    };
  }
}
