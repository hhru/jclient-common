package ru.hh.jclient.common.metrics;

import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.balancing.BalancingUpstreamManager;
import ru.hh.jclient.common.balancing.ConfigStore;
import ru.hh.jclient.common.balancing.JClientInfrastructureConfig;
import static ru.hh.jclient.common.balancing.PropertyKeys.ALLOW_CROSS_DC_KEY;
import static ru.hh.jclient.common.balancing.PropertyKeys.ALLOW_CROSS_DC_PATH;
import ru.hh.jclient.common.balancing.ServerStore;
import ru.hh.nab.metrics.StatsDSender;

public class MonitoringBalancingUpstreamManagerFactory {

  public static BalancingUpstreamManager createWithDefaults(
      JClientInfrastructureConfig infrastructureConfig,
      StatsDSender statsDSender,
      ConfigStore configStore,
      ServerStore serverStore,
      Properties strategyProperties,
      @Nullable Properties kafkaUpstreamMonitoringProperties
  ) {
    boolean allowCrossDCRequests = Optional
        .ofNullable(strategyProperties.getProperty(ALLOW_CROSS_DC_KEY))
        .or(() -> Optional.ofNullable(strategyProperties.getProperty(ALLOW_CROSS_DC_PATH)))
        .map(Boolean::parseBoolean)
        .orElse(false);
    return new BalancingUpstreamManager(
        configStore,
        serverStore,
        buildMonitoring(infrastructureConfig.getServiceName(), infrastructureConfig.getCurrentDC(), statsDSender, kafkaUpstreamMonitoringProperties),
        infrastructureConfig,
        allowCrossDCRequests
    );
  }

  private static Set<Monitoring> buildMonitoring(
      String serviceName,
      String dc,
      StatsDSender statsDSender,
      Properties kafkaUpstreamMonitoringProperties
  ) {
    Set<Monitoring> monitoring = new HashSet<>();
    KafkaUpstreamMonitoring.fromProperties(serviceName, dc, kafkaUpstreamMonitoringProperties).ifPresent(monitoring::add);
    monitoring.add(new UpstreamMonitoring(statsDSender, serviceName));

    return monitoring;
  }
}
