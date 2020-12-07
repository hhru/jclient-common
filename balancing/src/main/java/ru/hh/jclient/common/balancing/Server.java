package ru.hh.jclient.common.balancing;

import static java.util.Objects.requireNonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static ru.hh.jclient.common.balancing.AdaptiveBalancingStrategy.DOWNTIME_DETECTOR_WINDOW;
import static ru.hh.jclient.common.balancing.AdaptiveBalancingStrategy.RESPONSE_TIME_TRACKER_WINDOW;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class Server {
  private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
  private static final String DELIMITER = ":";

  private final String address;
  private volatile int weight;
  private volatile String datacenter;
  private volatile Map<String, String> meta;
  private volatile List<String> tags;

  private volatile boolean active = true;
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

  synchronized void release(boolean isError, long responseTimeMicros) {
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

  public synchronized void deactivate(int timeoutMs, ScheduledExecutorService executor) {
    LOGGER.info("deactivate server: {} for {}ms", address, timeoutMs);
    active = false;
    executor.schedule(this::activate, timeoutMs, TimeUnit.MILLISECONDS);
  }

  synchronized void activate() {
    LOGGER.info("activate server: {}", address);
    active = true;
    fails = 0;
    requests = 0;
    statsRequests = 0;
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

  public boolean isActive() {
    return active;
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
      ", active=" + active +
      ", requests=" + requests +
      ", fails=" + fails +
      ", statsRequests=" + statsRequests +
      '}';
  }
}
