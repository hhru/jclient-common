package ru.hh.jclient.common.metrics;

import ru.hh.jclient.common.Monitoring;
import ru.hh.metrics.StatsDSender;
import ru.hh.metrics.Tag;

import java.util.HashMap;
import java.util.Map;

public class UpstreamMonitoring implements Monitoring {
  private final StatsDSender statsDSender;
  private final String serviceName;

  public UpstreamMonitoring(StatsDSender statsDSender, String serviceName) {
    this.statsDSender = statsDSender;
    this.serviceName = serviceName;
  }

  @Override
  public void countRequest(String upstreamName, String serverAddress, long requestTimeMs, int statusCode, boolean isRequestFinal) {
    Map<String, String> tags = createCommonTags(serviceName, upstreamName, serverAddress, statusCode);
    tags.put("final", String.valueOf(isRequestFinal));
    tags.put("status", String.valueOf(statusCode));
    statsDSender.sendTiming("http.client.requests", requestTimeMs, toTagsArray(tags));
  }

  @Override
  public void countRetry(String upstreamName, String serverAddress, int statusCode, int firstStatusCode, int retryCount) {
    Map<String, String> tags = createCommonTags(serviceName, upstreamName, serverAddress, statusCode);
    tags.put("first_upstream_status", String.valueOf(firstStatusCode));
    tags.put("tries", String.valueOf(retryCount));
    statsDSender.sendCounter("http.client.retries", 1, toTagsArray(tags));
  }

  private static Map<String, String> createCommonTags(String serviceName, String upstreamName, String serverAddress, int statusCode) {
    Map<String, String> tags = new HashMap<>();
    tags.put("app", serviceName);
    tags.put("upstream", upstreamName);
    tags.put("server", serverAddress);
    tags.put("status", String.valueOf(statusCode));
    return tags;
  }

  private static Tag[] toTagsArray(Map<String, String> tags) {
    return tags.entrySet().stream().map(p -> new Tag(p.getKey(), p.getValue())).toArray(Tag[]::new);
  }
}
