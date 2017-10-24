package ru.hh.jclient.common.balancing;

import java.util.concurrent.atomic.AtomicInteger;

public class ServerCounter {
  private final AtomicInteger requests = new AtomicInteger(0);
  private final AtomicInteger fails = new AtomicInteger(0);

  synchronized void onAcquire() {
    requests.incrementAndGet();
  }

  synchronized void onRelease(boolean isError) {
    int currVal = requests.get();
    if (currVal > 0) {
      requests.compareAndSet(currVal, currVal - 1);
    }
    if (isError) {
      fails.incrementAndGet();
    } else {
      fails.set(0);
    }
  }

  synchronized void reset() {
    requests.set(0);
    fails.set(0);
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
}
