package ru.hh.jclient.common.balancing;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ServerCounter {
  private final AtomicInteger requests = new AtomicInteger(0);
  private final AtomicInteger fails = new AtomicInteger(0);
  private final AtomicLong totalRequests = new AtomicLong(0);
  private final AtomicLong totalFails = new AtomicLong(0);

  synchronized void onAcquire() {
    requests.incrementAndGet();
    totalRequests.incrementAndGet();
  }

  synchronized void onRelease(boolean isError) {
    int currVal = requests.get();
    if (currVal > 0) {
      requests.compareAndSet(currVal, currVal - 1);
    }
    if (isError) {
      fails.incrementAndGet();
      totalFails.incrementAndGet();
    } else {
      fails.set(0);
    }
  }

  synchronized void reset() {
    requests.set(0);
    fails.set(0);
    totalRequests.set(0);
    totalFails.set(0);
  }

  synchronized void resetTotals() {
    totalRequests.set(0);
    totalFails.set(0);
  }

  void resetFails() {
    fails.set(0);
  }

  public int getRequests() {
    return requests.get();
  }

  public int getFails() {
    return fails.get();
  }

  public long getTotalRequests() {
    return totalRequests.get();
  }

  public long getTotalFails() {
    return totalFails.get();
  }
}
