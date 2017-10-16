package ru.hh.jclient.common.balancing;

import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.metrics.Counters;
import ru.hh.metrics.StatsDSender;
import ru.hh.metrics.Tag;

public class UpstreamMonitoring {
  private final Tag serviceTag;
  private final Counters requestCounters;
  private final Counters retryCounters;

  public UpstreamMonitoring(StatsDSender statsDSender, String serviceName) {
    serviceTag = new Tag("app", serviceName);
    requestCounters = new Counters(500);
    retryCounters = new Counters(500);

    statsDSender.sendCountersPeriodically("http.client.requests", requestCounters);
    statsDSender.sendCountersPeriodically("http.client.retries", retryCounters);
  }

  public void addRequest(ResponseWrapper responseWrapper, String upstreamName, String serverAddress, boolean isFinal) {
    requestCounters.add(
        (int) responseWrapper.getTimeToLastByteMs(),
        serviceTag,
        new Tag("upstream", upstreamName),
        new Tag("server", serverAddress),
        new Tag("final", Boolean.toString(isFinal)),
        getStatusTag(responseWrapper));
  }

  public void addRetry(ResponseWrapper responseWrapper, String upstreamName, String serverAddress, int firstStatusCode, int retryCount) {
    retryCounters.add(
        1,
        serviceTag,
        new Tag("upstream", upstreamName),
        new Tag("server", serverAddress),
        new Tag("first_upstream_status", String.valueOf(firstStatusCode)),
        new Tag("tries", String.valueOf(retryCount)),
        getStatusTag(responseWrapper));
  }

  private static Tag getStatusTag(ResponseWrapper responseWrapper) {
    return new Tag("status", String.valueOf(responseWrapper.getResponse().getStatusCode()));
  }
}
