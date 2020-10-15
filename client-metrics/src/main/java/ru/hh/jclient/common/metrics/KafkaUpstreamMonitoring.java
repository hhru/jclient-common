package ru.hh.jclient.common.metrics;

import io.netty.util.internal.StringUtil;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.hh.jclient.common.Monitoring;

import java.util.Optional;
import java.util.Properties;

import static java.util.Optional.ofNullable;

public class KafkaUpstreamMonitoring implements Monitoring {
  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaUpstreamMonitoring.class);

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
  public void countRequest(String upstreamName, String dc, String hostname, int statusCode, long requestTimeMicros, boolean isRequestFinal) {
    if (isRequestFinal) {
      var requestId = ofNullable(MDC.get("rid")).orElse("");
      var jsonBuilder = new SimpleJsonBuilder();
      jsonBuilder.put("ts", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
      jsonBuilder.put("app", serviceName);
      jsonBuilder.put("upstream", upstreamName);
      jsonBuilder.put("dc", StringUtil.isNullOrEmpty(dc) ? localDc : dc);
      jsonBuilder.put("hostname", hostname);
      jsonBuilder.put("status", statusCode);
      jsonBuilder.put("requestId", requestId);
      String json = jsonBuilder.build();
      LOGGER.debug("Sending countRequest with json {}", json);
      ProducerRecord<String, String> record = new ProducerRecord<>(monitoringConfig.requestsCountTopicName, json);
      kafkaProducer.send(record, (recordMetadata, e) -> {
        if (e != null) {
          LOGGER.warn(e.getMessage(), e);
        }
      });
    }
  }

  @Override
  public void countRequestTime(String upstreamName, String dc, long requestTimeMicros) {

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
    final String requestsCountTopicName;

    Config(String requestsCountTopicName) {
      this.requestsCountTopicName = requestsCountTopicName;
    }

    static Config fromProperties(Properties properties) {
      var requestsCountTopicName = properties.getProperty("topics.requests");

      return new Config(requestsCountTopicName);
    }
  }
}
