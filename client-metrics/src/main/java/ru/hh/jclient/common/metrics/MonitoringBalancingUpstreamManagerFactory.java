package ru.hh.jclient.common.metrics;

import static ru.hh.jclient.common.balancing.PropertyKeys.ALLOWED_DEGRADATION_PART_KEY;
import static ru.hh.jclient.common.balancing.PropertyKeys.ALLOW_CROSS_DC_KEY;
import static ru.hh.jclient.common.balancing.PropertyKeys.ALLOW_CROSS_DC_PATH;

import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.balancing.BalancingUpstreamManager;
import ru.hh.jclient.common.balancing.ConfigStore;
import ru.hh.jclient.common.balancing.ServerStore;
import ru.hh.jclient.common.balancing.JClientInfrastructureConfig;
import ru.hh.nab.metrics.StatsDSender;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

public class MonitoringBalancingUpstreamManagerFactory {

  public static BalancingUpstreamManager createWithDefaults(JClientInfrastructureConfig infrastructureConfig,
                                                            StatsDSender statsDSender,
                                                            ConfigStore configStore,
                                                            ServerStore serverStore,
                                                            Properties strategyProperties,
                                                            @Nullable Properties kafkaUpstreamMonitoringProperties) {
    boolean allowCrossDCRequests = Optional.ofNullable(strategyProperties.getProperty(ALLOW_CROSS_DC_KEY))
      .or(() -> Optional.ofNullable(strategyProperties.getProperty(ALLOW_CROSS_DC_PATH)))
      .map(Boolean::parseBoolean)
      .orElse(false);
    double allowedUpstreamDegradationPart = Optional.ofNullable(strategyProperties.getProperty(ALLOWED_DEGRADATION_PART_KEY)).stream()
      .mapToDouble(Double::parseDouble).findFirst().orElse(0.5d);
    BalancingUpstreamManager.ValidationSettings validationSettings = new BalancingUpstreamManager.ValidationSettings()
      .setAllowedDegradationPart(allowedUpstreamDegradationPart);
    return new BalancingUpstreamManager(
      configStore, serverStore,
      buildMonitoring(infrastructureConfig.getServiceName(), infrastructureConfig.getCurrentDC(), statsDSender, kafkaUpstreamMonitoringProperties),
      infrastructureConfig, allowCrossDCRequests, validationSettings
    );
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
