package ru.hh.jclient.common.balancing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import static ru.hh.jclient.common.balancing.AdaptiveBalancingStrategy.DOWNTIME_DETECTOR_WINDOW;
import static ru.hh.jclient.common.balancing.AdaptiveBalancingStrategy.RESPONSE_TIME_TRACKER_WINDOW;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

public final class Server {
  private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
  private static final String DELIMITER = ":";

  private final String address;
  private final String datacenter;
  private volatile int weight;
  private volatile Map<String, String> meta;
  private volatile List<String> tags;

  private boolean oldGenServer;

  private volatile int warmupEndMillis = 0;
  private boolean warmupEnded;

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

  public Server setWeight(int weight) {
    this.weight = weight;
    return this;
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

  public float getCurrentLoad() {
    return (float) this.requests / this.weight;
  }

  public float getStatLoad(Collection<Server> currentServers, LongSupplier currentTimeMillisProvider) {
    if (!warmupEnded) {
      long currentTimeMillis = currentTimeMillisProvider.getAsLong();
      if (warmupEndMillis > 0 && currentTimeMillis <= warmupEndMillis) {
        LOGGER.trace("Warming up server {}. Current epoch millis: {}, warmup end epoch millis: {}", this, currentTimeMillis, warmupEndMillis);
        return Float.POSITIVE_INFINITY;
      }
      LOGGER.trace("Warm up for server {} complete", this);
      warmupEnded = true;
    }
    if (!oldGenServer && statsRequests == 0) {
      oldGenServer = true;
      statsRequests = calculateStatRequestsForMaxOfCurrentLoads(currentServers, weight);
      LOGGER.trace("Server {} is new. Set speculative statRequests={} and marked it as not new anymore", this, statsRequests);
    }
    return calculateStatLoad();
  }

  static int calculateStatRequestsForMaxOfCurrentLoads(Collection<Server> servers, int currentServerWeight) {
    return (int) Math.floor(servers.stream().mapToDouble(Server::calculateStatLoad).max().orElse(0d) * currentServerWeight);
  }

  private float calculateStatLoad() {
    return (float) this.statsRequests / this.weight;
  }

  public void setWarmupEndTimeIfNeeded(int slowStartSeconds, LongSupplier currentTimeMillisProvider) {
    if (warmupEndMillis == 0) {
      if (slowStartSeconds > 0) {
        long warmapEndTime = (long) (currentTimeMillisProvider.getAsLong() + (Math.random() * Duration.ofSeconds(slowStartSeconds).toMillis()));
        this.warmupEndMillis = Math.toIntExact(warmapEndTime);
        LOGGER.trace("Set warmup for server {}. Warmup is going to end at {} epoch millis", this, warmupEndMillis);
      } else {
        warmupEnded = true;
        this.warmupEndMillis = -1;
      }
    }
  }
}
