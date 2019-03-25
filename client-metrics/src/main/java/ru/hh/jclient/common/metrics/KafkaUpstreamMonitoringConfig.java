package ru.hh.jclient.common.metrics;

public class KafkaUpstreamMonitoringConfig {
  final String serviceName;
  final String dc;
  final long heartbeatPeriodInMillis;
  final String heartbeatTopicName;
  final String requestsCountTopicName;

  public KafkaUpstreamMonitoringConfig(String serviceName, String dc,
                                       long heartbeatPeriodInMillis,
                                       String heartbeatTopicName, String requestsCountTopicName) {
    this.serviceName = serviceName;
    this.dc = dc;
    this.heartbeatPeriodInMillis = heartbeatPeriodInMillis;
    this.heartbeatTopicName = heartbeatTopicName;
    this.requestsCountTopicName = requestsCountTopicName;
  }
}
