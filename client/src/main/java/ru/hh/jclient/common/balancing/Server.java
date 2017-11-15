package ru.hh.jclient.common.balancing;

import static java.util.Objects.requireNonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server {
  private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

  static final int DEFAULT_WEIGHT = 1;

  private final String address;
  private volatile int weight;

  private volatile boolean active = true;
  private volatile int requests = 0;
  private volatile int fails = 0;
  private volatile int statsRequests = 0;

  Server(String address, int weight) {
    this.address = requireNonNull(address, "address should not be null");
    this.weight = weight;
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

  synchronized void deactivate(int timeoutMs, ScheduledExecutorService executor) {
    LOGGER.info("deactivate server: {} for {}ms", address, timeoutMs);
    active = false;
    executor.schedule(this::activate, timeoutMs, TimeUnit.MILLISECONDS);
  }

  synchronized void activate() {
    LOGGER.info("activate server: {}", address);
    active = true;
    fails = 0;
  }

  synchronized void resetStatsRequests() {
    if (statsRequests >= weight && active) {
      statsRequests = 0;
    }
  }

  void update(Server server) {
    weight = server.weight;
  }

  String getAddress() {
    return address;
  }

  int getWeight() {
    return weight;
  }

  boolean isActive() {
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

  @Override
  public String toString() {
    return address + " (weight=" + weight + ")";
  }
}
