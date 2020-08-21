package ru.hh.jclient.common.metrics;

import ru.hh.jclient.consul.ConsulConfigService;
import ru.hh.jclient.consul.ConsulUpstreamService;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.balancing.BalancingRequestStrategy;
import ru.hh.jclient.common.balancing.BalancingUpstreamManager;
import ru.hh.jclient.common.balancing.RequestBalancerBuilder;
import ru.hh.jclient.common.balancing.UpstreamManager;
import ru.hh.nab.metrics.StatsDSender;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class MonitoringUpstreamManagerFactory {
  public static RequestStrategy<RequestBalancerBuilder> create(
      String serviceName, String dc,
      boolean allowCrossDCRequests,
      StatsDSender statsDSender, @Nullable Properties kafkaUpstreamMonitoringProperties,
      ScheduledExecutorService scheduledExecutorService,
      Consumer<UpstreamManager> upstreamUpdater,
  ConsulConfigService consulConfigService
          , ConsulUpstreamService consulUpstreamService) {
    var balancingUpstreamManager = new BalancingUpstreamManager(
      scheduledExecutorService,
      buildMonitoring(serviceName, dc, statsDSender, kafkaUpstreamMonitoringProperties),
      dc, allowCrossDCRequests, consulConfigService, consulUpstreamService
    );

    upstreamUpdater.accept(balancingUpstreamManager);

    return new BalancingRequestStrategy(balancingUpstreamManager);
  }

  private static Set<Monitoring> buildMonitoring(String serviceName, String dc, StatsDSender statsDSender,
                                                 Properties kafkaUpstreamMonitoringProperties) {
    Set<Monitoring> monitoring = new HashSet<>();

    KafkaUpstreamMonitoring.fromProperties(serviceName, dc, kafkaUpstreamMonitoringProperties)
      .ifPresent(monitoring::add);

    monitoring.add(new UpstreamMonitoring(statsDSender, serviceName));

    return monitoring;
  }
}
