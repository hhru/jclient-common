package ru.hh.jclient.common.metrics;

import static java.util.Optional.ofNullable;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.balancing.BalancingRequestStrategy;
import ru.hh.jclient.common.balancing.BalancingUpstreamManager;
import ru.hh.jclient.common.balancing.RequestBalancerBuilder;
import ru.hh.jclient.consul.UpstreamConfigService;
import ru.hh.jclient.consul.UpstreamService;
import ru.hh.nab.metrics.StatsDSender;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class MonitoringRequestStrategyFactory {
  public static RequestStrategy<RequestBalancerBuilder> create(
      String serviceName, String dc,
      StatsDSender statsDSender,
      UpstreamConfigService upstreamConfigService,
      UpstreamService upstreamService,
      List<String> upstreamList,
      boolean allowCrossDCRequests,
      @Nullable Properties kafkaUpstreamMonitoringProperties,
      @Nullable Double allowedUpstreamDegradationPart) {
    var balancingUpstreamManager = new BalancingUpstreamManager(
      upstreamList,
      buildMonitoring(serviceName, dc, statsDSender, kafkaUpstreamMonitoringProperties),
      dc, allowCrossDCRequests, upstreamConfigService, upstreamService, ofNullable(allowedUpstreamDegradationPart).orElse(0.5)
    );
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
