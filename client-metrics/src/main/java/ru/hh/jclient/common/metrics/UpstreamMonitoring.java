package ru.hh.jclient.common.metrics;

import ru.hh.jclient.common.Monitoring;
import ru.hh.metrics.StatsDSender;
import ru.hh.metrics.Tag;

import java.util.HashMap;
import java.util.Map;

/**
 * UpstreamMonitoring sends requests metrics to okmeter.io using StatsD.
 *
 * Metrics:
 *  - http.client.requests
 *  - http.client.request.time
 *  - http.client.retries
 */
public class UpstreamMonitoring implements Monitoring {
  private final StatsDSender statsDSender;
  private final String serviceName;

  public UpstreamMonitoring(StatsDSender statsDSender, String serviceName) {
    this.statsDSender = statsDSender;
    this.serviceName = serviceName;
  }

  @Override
  public void countRequest(String upstreamName, String serverAddress, int statusCode, boolean isRequestFinal) {
    Map<String, String> tags = getCommonTags(serviceName, upstreamName);
    tags.put("server", serverAddress);
    tags.put("status", String.valueOf(statusCode));
    tags.put("final", String.valueOf(isRequestFinal));
    statsDSender.sendCounter("http.client.requests", 1, toTagsArray(tags));
  }

  @Override
  public void countRequestTime(String upstreamName, long requestTimeMs) {
    Map<String, String> tags = getCommonTags(serviceName, upstreamName);
    statsDSender.sendTiming("http.client.request.time", requestTimeMs, toTagsArray(tags));
  }

  @Override
  public void countRetry(String upstreamName, String serverAddress, int statusCode, int firstStatusCode, int retryCount) {
    Map<String, String> tags = getCommonTags(serviceName, upstreamName);
    tags.put("server", serverAddress);
    tags.put("status", String.valueOf(statusCode));
    tags.put("first_upstream_status", String.valueOf(firstStatusCode));
    tags.put("tries", String.valueOf(retryCount));
    statsDSender.sendCounter("http.client.retries", 1, toTagsArray(tags));
  }

  private static Map<String, String> getCommonTags(String serviceName, String upstreamName) {
    Map<String, String> tags = new HashMap<>();
    tags.put("app", serviceName);
    tags.put("upstream", upstreamName);
    return tags;
  }

  private static Tag[] toTagsArray(Map<String, String> tags) {
    return tags.entrySet().stream().map(p -> new Tag(p.getKey(), p.getValue())).toArray(Tag[]::new);
  }
}
