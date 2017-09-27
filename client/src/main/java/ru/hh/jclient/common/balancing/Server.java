package ru.hh.jclient.common.balancing;

import static java.util.Objects.requireNonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
  static final int DEFAULT_WEIGHT = 1;
  private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

  private final String address;
  private final AtomicBoolean active = new AtomicBoolean(true);
  private final ServerCounter counter = new ServerCounter();

  private volatile int weight;

  Server(String address, int weight) {
    this.address = requireNonNull(address, "address should not be null");
    this.weight = weight;
  }

  void suspend(int timeoutMs, ScheduledExecutorService executor) {
    if (active.compareAndSet(true, false)) {
      LOGGER.info("suspending server: {} for {}ms", address, timeoutMs);
      executor.schedule(this::resume, timeoutMs, TimeUnit.MILLISECONDS);
    }
  }

  private void resume() {
    if (active.compareAndSet(false, true)) {
      LOGGER.info("resuming server: {}", address);
      counter.resetFails();
    }
  }

  void update(Server server) {
    this.weight = server.weight;
  }

  String getAddress() {
    return address;
  }

  int getWeight() {
    return weight;
  }

  boolean isActive() {
    return active.get();
  }

  ServerCounter getCounter() {
    return counter;
  }

  @Override
  public String toString() {
    return address + " (weight=" + weight + ")";
  }
}
