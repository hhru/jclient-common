package ru.hh.jclient.common.balancing;

import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static java.util.Objects.requireNonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static ru.hh.jclient.common.balancing.AdaptiveBalancingStrategy.DOWNTIME_DETECTOR_WINDOW;
import static ru.hh.jclient.common.balancing.AdaptiveBalancingStrategy.RESPONSE_TIME_TRACKER_WINDOW;

//TODO move to ru.hh.jclient.common.balancing.internal
public class Server {
  private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
  private static final String DELIMITER = ":";

  private final String address;
  private final String datacenter;
  private volatile int weight;
  private volatile Map<String, String> meta;
  private volatile List<String> tags;

  /**
   * not volatile for optimization. Should protect writes with {@link Server#slowStartEndMillis}
   */
  private boolean slowStartModeEnabled;
  private volatile long slowStartEndMillis = 0;

  /**
   * not volatile for optimization. Should protect writes with {@link Server#statsRequests}, {@link Server#requests} or {@link Server#fails}
   */
  private boolean statisticsFilledWithInitialValues;
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

  synchronized void rescaleStatsRequests(Collection<Server> allServers) {
    statsRequests -= weight;
    if (statsRequests > weight * 2) {
      LOGGER.warn("Rescaled server {}. Something wrong - too big stat reminder. Servers: {} ", this, allServers);
    }
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

  public float getStatLoad(Collection<Server> currentServers, Clock clock) {
    if (slowStartModeEnabled) {
      long currentTimeMillis = getCurrentTimeMillis(clock);
      if (slowStartEndMillis > 0 && currentTimeMillis <= slowStartEndMillis) {
        LOGGER.trace(
          "Server {} is on slowStart, returning infinite load. Current epoch millis: {}, slow start end epoch millis: {}",
          this, currentTimeMillis, slowStartEndMillis
        );
        return Float.POSITIVE_INFINITY;
      }
      LOGGER.trace("Slow start for server {} ended", this);
      slowStartModeEnabled = false;
    }
    if (!statisticsFilledWithInitialValues && statsRequests <= 0) {
      statisticsFilledWithInitialValues = true;
      statsRequests = calculateStatRequestsForMaxOfCurrentLoads(currentServers, weight);
      LOGGER.trace("Server {} statistics has no init value. Calculated initial statRequests={}", this, statsRequests);
    }
    return calculateStatLoad();
  }

  static int calculateStatRequestsForMaxOfCurrentLoads(Collection<Server> servers, int currentServerWeight) {
    return (int) Math.floor(servers.stream().mapToDouble(Server::calculateStatLoad).max().orElse(0d) * currentServerWeight);
  }

  private float calculateStatLoad() {
    return (float) this.statsRequests / this.weight;
  }

  protected long getCurrentTimeMillis(Clock clock) {
    return clock.millis();
  }

  public void setSlowStartEndTimeIfNeeded(int slowStartSeconds, Clock clock) {
    if (slowStartSeconds > 0) {
      if (slowStartEndMillis == 0) {
        slowStartModeEnabled = true;
        this.slowStartEndMillis = (long) (getCurrentTimeMillis(clock) + (Math.random() * Duration.ofSeconds(slowStartSeconds).toMillis()));
        LOGGER.trace("Set slow start for server {}. Slow start is going to end at {} epoch millis", this, slowStartEndMillis);
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Server server = (Server) o;
    return address.equals(server.address) && Objects.equals(datacenter, server.datacenter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(address, datacenter);
  }
}
