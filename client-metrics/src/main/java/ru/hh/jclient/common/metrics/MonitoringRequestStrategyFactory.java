package ru.hh.jclient.common.metrics;

import static ru.hh.jclient.consul.PropertyKeys.ALLOWED_DEGRADATION_PART_KEY;
import static ru.hh.jclient.consul.PropertyKeys.ALLOW_CROSS_DC_KEY;
import static ru.hh.jclient.consul.PropertyKeys.ALLOW_CROSS_DC_PATH;
import static ru.hh.jclient.consul.PropertyKeys.UPSTREAMS_KEY;

import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.balancing.BalancingRequestStrategy;
import ru.hh.jclient.common.balancing.BalancingUpstreamManager;
import ru.hh.jclient.common.balancing.RequestBalancerBuilder;
import ru.hh.jclient.consul.UpstreamConfigService;
import ru.hh.jclient.consul.UpstreamService;
import ru.hh.jclient.consul.model.config.JClientInfrastructureConfig;
import ru.hh.nab.metrics.StatsDSender;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

public class MonitoringRequestStrategyFactory {

  public static RequestStrategy<RequestBalancerBuilder> createWithDefaults(JClientInfrastructureConfig infrastructureConfig,
                                                                           StatsDSender statsDSender,
                                                                           UpstreamConfigService upstreamConfigService,
                                                                           UpstreamService upstreamService,
                                                                           Properties strategyProperties,
                                                                           @Nullable Properties kafkaUpstreamMonitoringProperties) {
    var upstreamList = Optional.ofNullable(strategyProperties.getProperty(UPSTREAMS_KEY))
      .filter(Predicate.not(String::isBlank))
      .map(separatedList -> List.of(separatedList.split("[,\\s]+")))
      .orElseGet(List::of);
    boolean allowCrossDCRequests = Optional.ofNullable(strategyProperties.getProperty(ALLOW_CROSS_DC_KEY))
        .or(() -> Optional.ofNullable(strategyProperties.getProperty(ALLOW_CROSS_DC_PATH)))
        .map(Boolean::parseBoolean)
        .orElse(false);
    double allowedUpstreamDegradationPart = Optional.ofNullable(strategyProperties.getProperty(ALLOWED_DEGRADATION_PART_KEY)).stream()
      .mapToDouble(Double::parseDouble).findFirst().orElse(0.5d);
    var balancingUpstreamManager = new BalancingUpstreamManager(
      upstreamList,
      buildMonitoring(infrastructureConfig.getServiceName(), infrastructureConfig.getCurrentDC(), statsDSender, kafkaUpstreamMonitoringProperties),
      infrastructureConfig, allowCrossDCRequests, upstreamConfigService, upstreamService, allowedUpstreamDegradationPart
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
