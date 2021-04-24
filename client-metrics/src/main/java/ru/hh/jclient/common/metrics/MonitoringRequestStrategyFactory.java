package ru.hh.jclient.common.metrics;

import static ru.hh.jclient.common.balancing.PropertyKeys.ALLOWED_DEGRADATION_PART_KEY;
import static ru.hh.jclient.common.balancing.PropertyKeys.ALLOW_CROSS_DC_KEY;
import static ru.hh.jclient.common.balancing.PropertyKeys.ALLOW_CROSS_DC_PATH;
import static ru.hh.jclient.common.balancing.PropertyKeys.IGNORE_NO_SERVERS_IN_CURRENT_DC_KEY;
import static ru.hh.jclient.common.balancing.PropertyKeys.SYNC_UPDATE_KEY;
import static ru.hh.jclient.common.balancing.PropertyKeys.UPSTREAMS_KEY;

import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.balancing.BalancingRequestStrategy;
import ru.hh.jclient.common.balancing.BalancingUpstreamManager;
import ru.hh.jclient.common.balancing.ConfigStore;
import ru.hh.jclient.common.balancing.RequestBalancerBuilder;
import ru.hh.jclient.common.balancing.ServerStore;
import ru.hh.jclient.common.balancing.JClientInfrastructureConfig;
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
                                                                           ConfigStore configStore,
                                                                           ServerStore serverStore,
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
    boolean ignoreNoServersInCurrentDC = Optional.ofNullable(strategyProperties.getProperty(IGNORE_NO_SERVERS_IN_CURRENT_DC_KEY))
      .map(Boolean::parseBoolean)
      .orElse(false);
    boolean failOnEmptyUpstreams = Optional.ofNullable(strategyProperties.getProperty(SYNC_UPDATE_KEY))
      .map(Boolean::parseBoolean)
      .orElse(true);
    BalancingUpstreamManager.ValidationSettings validationSettings = new BalancingUpstreamManager.ValidationSettings()
      .setAllowedDegradationPart(allowedUpstreamDegradationPart)
      .setIgnoreNoServersInCurrentDC(ignoreNoServersInCurrentDC)
      .setFailOnEmptyUpstreams(failOnEmptyUpstreams);
    var balancingUpstreamManager = new BalancingUpstreamManager(
      configStore, serverStore,
      buildMonitoring(infrastructureConfig.getServiceName(), infrastructureConfig.getCurrentDC(), statsDSender, kafkaUpstreamMonitoringProperties),
      infrastructureConfig, allowCrossDCRequests, validationSettings
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
