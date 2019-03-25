package ru.hh.jclient.common.metrics;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.hh.jclient.common.Monitoring;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KafkaUpstreamMonitoring implements Monitoring {
  private static final Logger log = LoggerFactory.getLogger(KafkaUpstreamMonitoring.class);

  private final KafkaProducer<String, String> kafkaProducer;
  private final KafkaUpstreamMonitoringConfig monitoringConfig;

  public KafkaUpstreamMonitoring(KafkaProducer<String, String> kafkaMetricsProducer,
                                 KafkaUpstreamMonitoringConfig monitoringConfig) {
    this.kafkaProducer = kafkaMetricsProducer;
    this.monitoringConfig = monitoringConfig;
  }

  public void startHeartbeat(ScheduledExecutorService executorService) throws UnknownHostException {
    final String hostname = InetAddress.getLocalHost().getHostName();

    executorService.scheduleAtFixedRate(() -> {
      var jsonBuilder = new SimpleJsonBuilder();
      jsonBuilder.put("app", monitoringConfig.serviceName);
      jsonBuilder.put("hostname", hostname);
      jsonBuilder.put("dc", monitoringConfig.dc);
      jsonBuilder.put("ts", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
      ProducerRecord<String, String> record = new ProducerRecord<>(monitoringConfig.heartbeatTopicName, jsonBuilder.build());
      kafkaProducer.send(record, (recordMetadata, e) -> {
        if (e != null) {
          log.warn(e.getMessage(), e);
        }
      });
    }, 0, monitoringConfig.heartbeatPeriodInMillis, TimeUnit.MILLISECONDS);
  }

  @Override
  public void countRequest(String upstreamName, String dc, String serverAddress, int statusCode, long requestTimeMs, boolean isRequestFinal) {
    var requestId = MDC.get("rid");
    var jsonBuilder = new SimpleJsonBuilder();
    jsonBuilder.put("app", monitoringConfig.serviceName);
    jsonBuilder.put("upstream", upstreamName);
    jsonBuilder.put("dc", dc);
    jsonBuilder.put("hostname", serverAddress);
    jsonBuilder.put("ts", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
    jsonBuilder.put("requestId", requestId);
    ProducerRecord<String, String> record = new ProducerRecord<>(monitoringConfig.requestsCountTopicName, jsonBuilder.build());
    kafkaProducer.send(record, (recordMetadata, e) -> {
      if (e != null) {
        log.warn(e.getMessage(), e);
      }
    });
  }

  @Override
  public void countRequestTime(String upstreamName, String dc, long requestTimeMs) {

  }

  @Override
  public void countRetry(String upstreamName, String dc, String serverAddress, int statusCode, int firstStatusCode, int retryCount) {

  }

}
