package ru.hh.jclient.common.metrics;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import ru.hh.jclient.common.Monitoring;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InfluxUpstreamMonitoring implements Monitoring {

  private final String serviceName;
  private final String dc;
  private final InfluxDB influxDB;

  public InfluxUpstreamMonitoring(String serviceName,
                                  String dc,
                                  InfluxDB influxDB) {
    this.serviceName = serviceName;
    this.dc = dc;
    this.influxDB = influxDB;
  }

  public void startHeartbeat(ScheduledExecutorService executorService, long period) throws UnknownHostException {
    final String hostname = InetAddress.getLocalHost().getHostName();
    executorService.scheduleAtFixedRate(() -> influxDB.write(Point.measurement("heartbeat")
      .tag("app", serviceName)
      .tag("hostname", hostname)
      .tag("dc", dc)
      .addField("ts", System.currentTimeMillis())
      .build()), 0, period, TimeUnit.MILLISECONDS);
  }

  @Override
  public void countRequest(String upstreamName, String serverDataCenter, String serverAddress,
                           int statusCode,
                           long requestTimeMs,
                           boolean isRequestFinal) {
    if (statusCode >= 500 && isRequestFinal) {
      influxDB.write(
        Point.measurement("request")
          .addField("response_time", requestTimeMs)
          .tag("server", serverAddress)
          .tag("status", String.valueOf(statusCode))
          .tag(getCommonTags(serviceName, upstreamName, serverDataCenter))
          .build());
    }
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
