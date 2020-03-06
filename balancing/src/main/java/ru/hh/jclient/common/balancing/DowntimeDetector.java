package ru.hh.jclient.common.balancing;

final class DowntimeDetector {
  private final int[] errors;
  private final int n;

  private volatile int errorsCount;
  private int current;

  DowntimeDetector(int n) {
    this.n = n;
    errors = new int[n];
    current = 0;
    errorsCount = 0;
  }

  public synchronized void failed() {
    errorsCount = errorsCount + 1 - errors[current];
    errors[current] = 1;
    current = (current + 1) % n;
  }

  public synchronized void success() {
    errorsCount -= errors[current];
    errors[current] = 0;
    current = (current + 1) % n;
  }

  public int successCount() {
    return n - errorsCount;
  }
}
