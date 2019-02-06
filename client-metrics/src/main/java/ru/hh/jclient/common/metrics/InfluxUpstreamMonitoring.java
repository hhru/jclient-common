package ru.hh.jclient.common.metrics;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import ru.hh.jclient.common.Monitoring;

import java.util.HashMap;
import java.util.Map;

public class InfluxUpstreamMonitoring implements Monitoring {

  private final String serviceName;
  private final InfluxDB influxDB;

  public InfluxUpstreamMonitoring(String serviceName, InfluxDB influxDB) {
    this.serviceName = serviceName;
    this.influxDB = influxDB;
  }

  @Override
  public void countRequest(String upstreamName, String serverDataCenter, String serverAddress,
                           int statusCode,
                           long requestTimeMs,
                           boolean isRequestFinal) {
    influxDB.write(
      Point.measurement("request")
        .addField("response_time", requestTimeMs)
        .tag("server", serverAddress)
        .tag("status", String.valueOf(statusCode))
        .tag("final", String.valueOf(isRequestFinal))
        .tag(getCommonTags(serviceName, upstreamName, serverDataCenter))
        .build());
  }

  @Override
  public void countRequestTime(String upstreamName, String serverDataCenter, long requestTimeMs) {
  }

  @Override
  public void countRetry(String upstreamName, String serverDataCenter, String serverAddress, int statusCode, int firstStatusCode, int retryCount) {
  }

  private static Map<String, String> getCommonTags(String serviceName, String upstreamName, String dataCenter) {
    Map<String, String> tags = new HashMap<>();
    tags.put("app", serviceName);
    tags.put("upstream", upstreamName);
    tags.put("dc", dataCenter);
    return tags;
  }
}
