package ru.hh.jclient.common.balancing;

import static java.util.Objects.requireNonNull;
import static ru.hh.jclient.common.balancing.AdaptiveBalancingStrategy.DOWNTIME_DETECTOR_WINDOW;
import static ru.hh.jclient.common.balancing.AdaptiveBalancingStrategy.RESPONSE_TIME_TRACKER_WINDOW;

import java.util.List;
import java.util.Map;

public final class Server {
  private static final String DELIMITER = ":";

  private final String address;
  private final int weight;
  private final String datacenter;
  private volatile Map<String, String> meta;
  private volatile List<String> tags;

  private volatile int requests = 0;
  private volatile int fails = 0;
  private volatile int statsRequests = 0;

  private final DowntimeDetector downtimeDetector;
  private final ResponseTimeTracker responseTimeTracker;

  public Server(String address, int weight, String datacenter) {
    this.address = requireNonNull(address, "address should not be null");
    this.weight = weight;
    this.datacenter = datacenter;

    this.downtimeDetector = new DowntimeDetector(DOWNTIME_DETECTOR_WINDOW);
    this.responseTimeTracker = new ResponseTimeTracker(RESPONSE_TIME_TRACKER_WINDOW);
  }

  public static String addressFromHostPort(String host, int port) {
    return host + DELIMITER + port;
  }

  synchronized void acquire() {
    requests++;
    statsRequests++;
  }

  synchronized void release(boolean isError) {
    if (requests > 0) {
      requests--;
    }
    if (isError) {
      fails++;
    } else {
      fails = 0;
    }
  }

  void releaseAdaptive(boolean isError, long responseTimeMicros) {
    if (isError) {
      downtimeDetector.failed();
    } else {
      downtimeDetector.success();
      responseTimeTracker.time(responseTimeMicros);
    }
  }

  synchronized void rescaleStatsRequests() {
    statsRequests -= weight;
  }

  public String getAddress() {
    return address;
  }

  public int getWeight() {
    return weight;
  }

  public String getDatacenter() {
    return datacenter;
  }

  public String getDatacenterLowerCased() {
    return datacenter == null ? null : datacenter.toLowerCase();
  }

  public int getRequests() {
    return requests;
  }

  public int getFails() {
    return fails;
  }

  public int getStatsRequests() {
    return statsRequests;
  }

  public DowntimeDetector getDowntimeDetector() {
    return downtimeDetector;
  }

  public ResponseTimeTracker getResponseTimeTracker() {
    return responseTimeTracker;
  }

  public Map<String, String> getMeta() {
    return meta;
  }

  public void setMeta(Map<String, String> meta) {
    this.meta = meta;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  @Override
  public String toString() {
    return "Server{" +
      "address='" + address + '\'' +
      ", weight=" + weight +
      ", datacenter='" + datacenter + '\'' +
      ", meta=" + meta +
      ", tags=" + tags +
      ", requests=" + requests +
      ", fails=" + fails +
      ", statsRequests=" + statsRequests +
      '}';
  }
}
