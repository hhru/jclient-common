package ru.hh.jclient.common.balancing;

public class ResponseTimeTracker {
  private final long[] times;
  private final int n;

  private volatile long total;
  private volatile boolean isWarmUp = true;
  private int current;

  public ResponseTimeTracker(int n) {
    this.n = n;
    times = new long[n];
  }

  public synchronized void time(long time) {
    total += time - times[current];
    times[current] = time;
    if (current == n - 1 && isWarmUp) {
      isWarmUp = false;
    }
    current = (current + 1) % n;
  }

  public long mean() {
    return total / n;
  }

  public boolean isWarmUp() {
    return isWarmUp;
  }
}
