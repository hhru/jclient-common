package ru.hh.jclient.common;

import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpProvider;
import com.ning.http.client.AsyncHttpProviderConfig;
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;
import com.ning.http.client.providers.netty.channel.ChannelManager;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.metric.MetricProvider;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

public final class MetricProviderFactory {

  private static final Logger log = LoggerFactory.getLogger(MetricProviderFactory.class);

  private MetricProviderFactory() {
  }

  public static MetricProvider from(HttpClientBuilder clientBuilder) {
    AsyncHttpClientConfig config = clientBuilder.getHttp().getConfig();
    AsyncHttpProvider provider = clientBuilder.getHttp().getProvider();
    DefaultChannelGroup channelInfo = getChannelInfo(provider);
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
        return () -> Optional.ofNullable(((ThreadPoolExecutor) applicationExecutorService).getQueue()).map(Queue::size).orElse(-1);
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
      public Supplier<Integer> nettyBossThreadPoolQueueSizeProvider() {
        return () -> Optional.ofNullable(((ThreadPoolExecutor) nettyBossExecutorService).getQueue()).map(Queue::size).orElse(-1);
      }

      @Override
      public Supplier<Integer> nettyChannelPoolSizeProvider() {
        return channelInfo::size;
      }

      @Override
      public boolean containsApplicationThreadPoolMetrics() {
        return applicationExecutorServiceMeasurable;
      }

      @Override
      public boolean containsNettyBossThreadPoolMetrics() {
        return nettyBossExecutorServiceMeasurable;
      }

      @Override
      public boolean containsNettyChannelMetrics() {
        return channelInfo != null;
      }
    };
  }

  private static DefaultChannelGroup getChannelInfo(AsyncHttpProvider provider) {
    try {
      Field channelManagerField = NettyAsyncHttpProvider.class.getDeclaredField("channelManager");
      channelManagerField.setAccessible(true);
      Object channelManager = channelManagerField.get(provider);
      if (channelManager == null) {
        return null;
      }
      Field openChannelsField = ChannelManager.class.getDeclaredField("openChannels");
      openChannelsField.setAccessible(true);
      Object openChannels = openChannelsField.get(channelManager);
      if (!(openChannels instanceof DefaultChannelGroup)) {
        return null;
      }
      return (DefaultChannelGroup) openChannels;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      log.warn("Error on getting channelInfo", e);
      return null;
    }
  }

  private static ExecutorService extract(AsyncHttpProviderConfig<?, ?> asyncHttpProviderConfig) {
    return ((NettyAsyncHttpProviderConfig) asyncHttpProviderConfig).getBossExecutorService();
  }
}
