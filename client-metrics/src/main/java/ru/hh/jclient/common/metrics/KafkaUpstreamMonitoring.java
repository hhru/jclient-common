package ru.hh.jclient.common.metrics;

import io.netty.util.internal.StringUtil;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.hh.jclient.common.Monitoring;

import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

public class KafkaUpstreamMonitoring implements Monitoring {
  private static final Logger log = LoggerFactory.getLogger(KafkaUpstreamMonitoring.class);

  private final String serviceName;
  private final String localDc;
  private final KafkaProducer<String, String> kafkaProducer;
  private final Config monitoringConfig;

  public KafkaUpstreamMonitoring(String serviceName, String localDc,
                                 KafkaProducer<String, String> kafkaMetricsProducer,
                                 Config monitoringConfig) {
    this.serviceName = serviceName;
    this.localDc = localDc;
    this.kafkaProducer = kafkaMetricsProducer;
    this.monitoringConfig = monitoringConfig;
  }

  @Override
  public void countRequest(String upstreamName, String dc, String serverAddress, int statusCode, long requestTimeMs, boolean isRequestFinal) {
    if (isRequestFinal) {
      var requestId = ofNullable(MDC.get("rid")).orElse("");
      var jsonBuilder = new SimpleJsonBuilder();
      jsonBuilder.put("ts", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
      jsonBuilder.put("app", serviceName);
      jsonBuilder.put("upstream", upstreamName);
      jsonBuilder.put("dc", StringUtil.isNullOrEmpty(dc) ? localDc : dc);
      jsonBuilder.put("server", serverAddress);
      jsonBuilder.put("status", statusCode);
      jsonBuilder.put("requestId", requestId);
      ProducerRecord<String, String> record = new ProducerRecord<>(monitoringConfig.requestsCountTopicName, jsonBuilder.build());
      kafkaProducer.send(record, (recordMetadata, e) -> {
        if (e != null) {
          log.warn(e.getMessage(), e);
        }
      });
    }
  }

  @Override
  public void countRequestTime(String upstreamName, String dc, long requestTimeMs) {

  }

  @Override
  public void countRetry(String upstreamName, String dc, String serverAddress, int statusCode, int firstStatusCode, int retryCount) {

  }

  public static Optional<KafkaUpstreamMonitoring> fromProperties(String serviceName, String dc, Properties properties) {
    return ofNullable(properties)
      .map(props -> props.getProperty("enabled")).map(Boolean::parseBoolean)
      .filter(Boolean.TRUE::equals)
      .map(b -> new KafkaProducer<>(properties, new StringSerializer(), new StringSerializer()))
      .map(producer -> new KafkaUpstreamMonitoring(serviceName, dc, producer, Config.fromProperties(properties)));
  }

  static class Config {
    final long heartbeatPeriodInMillis;
    final String heartbeatTopicName;
    final String requestsCountTopicName;

    Config(long heartbeatPeriodInMillis, String heartbeatTopicName, String requestsCountTopicName) {
      this.heartbeatPeriodInMillis = heartbeatPeriodInMillis;
      this.heartbeatTopicName = heartbeatTopicName;
      this.requestsCountTopicName = requestsCountTopicName;
    }

    static Config fromProperties(Properties properties) {
      var requestsCountTopicName = properties.getProperty("topics.requests");
      var heartbeatTopicName = properties.getProperty("topics.heartbeat");
      var heartbeatPeriodInMillis = Long.parseLong(properties.getProperty("heartbeat.period.ms", "10000"));

      return new Config(heartbeatPeriodInMillis, heartbeatTopicName, requestsCountTopicName);
    }
  }
}
