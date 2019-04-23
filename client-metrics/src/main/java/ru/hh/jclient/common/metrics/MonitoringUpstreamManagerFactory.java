package ru.hh.jclient.common.metrics;

import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.UpstreamManager;
import ru.hh.jclient.common.balancing.BalancingUpstreamManager;
import ru.hh.nab.metrics.StatsDSender;

import javax.annotation.Nullable;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class MonitoringUpstreamManagerFactory {
  public static UpstreamManager create(String serviceName, String dc, boolean allowCrossDCRequests,
                                       StatsDSender statsDSender, @Nullable Properties kafkaUpstreamMonitoringProperties,
                                       ScheduledExecutorService scheduledExecutorService,
                                       Consumer<BalancingUpstreamManager> upstreamUpdater) {
    var balancingUpstreamManager = new BalancingUpstreamManager(
      scheduledExecutorService,
      buildMonitoring(serviceName, dc, statsDSender, kafkaUpstreamMonitoringProperties, scheduledExecutorService),
      dc, allowCrossDCRequests
    );

    upstreamUpdater.accept(balancingUpstreamManager);

    return balancingUpstreamManager;
  }

  private static Set<Monitoring> buildMonitoring(String serviceName, String dc, StatsDSender statsDSender,
                                                 Properties kafkaUpstreamMonitoringProperties,
                                                 ScheduledExecutorService scheduledExecutorService) {
    Set<Monitoring> monitoring = new HashSet<>();

    KafkaUpstreamMonitoring.fromProperties(serviceName, dc, kafkaUpstreamMonitoringProperties)
      .ifPresent(kafkaUpstreamMonitoring -> {
        try {
          kafkaUpstreamMonitoring.startHeartbeat(scheduledExecutorService);
          monitoring.add(kafkaUpstreamMonitoring);
        } catch (UnknownHostException e) {
          throw new RuntimeException(e);
        }
      });

    monitoring.add(new UpstreamMonitoring(statsDSender, serviceName));

    return monitoring;
  }
}
