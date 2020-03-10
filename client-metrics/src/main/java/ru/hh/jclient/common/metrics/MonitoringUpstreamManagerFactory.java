package ru.hh.jclient.common.metrics;

import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.balancing.BalancingRequestStrategy;
import ru.hh.jclient.common.balancing.BalancingUpstreamManager;
import ru.hh.jclient.common.balancing.RequestBalancer;
import ru.hh.nab.metrics.StatsDSender;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class MonitoringUpstreamManagerFactory {
  public static RequestStrategy<RequestBalancer> create(
      String serviceName, String dc, boolean allowCrossDCRequests,
      StatsDSender statsDSender, @Nullable Properties kafkaUpstreamMonitoringProperties,
      ScheduledExecutorService scheduledExecutorService,
      Consumer<BalancingUpstreamManager> upstreamUpdater) {
    return create(serviceName, dc,
            allowCrossDCRequests, false,
            statsDSender, kafkaUpstreamMonitoringProperties,
            scheduledExecutorService, upstreamUpdater);

  }
  public static RequestStrategy<RequestBalancer> create(
      String serviceName, String dc,
      boolean allowCrossDCRequests, boolean skipAdaptiveProfileSelection,
      StatsDSender statsDSender, @Nullable Properties kafkaUpstreamMonitoringProperties,
      ScheduledExecutorService scheduledExecutorService,
      Consumer<BalancingUpstreamManager> upstreamUpdater) {
    var balancingUpstreamManager = new BalancingUpstreamManager(
      scheduledExecutorService,
      buildMonitoring(serviceName, dc, statsDSender, kafkaUpstreamMonitoringProperties),
      dc, allowCrossDCRequests, skipAdaptiveProfileSelection
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
