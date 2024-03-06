package ru.hh.jclient.common.balancing;

public class DowntimeDetector {
  private final int[] errors;
  private final int n;

  private volatile int errorsCount;
  private int current;

  public DowntimeDetector(int n) {
    this(n, false);
  }

  public DowntimeDetector(int n, boolean isInitiallyDown) {
    this.n = n;
    errors = new int[n];
    current = 0;
    if (isInitiallyDown) {
      errorsCount = n;
      for (int i = 0; i < n; i++) {
        errors[i] = 1;
      }
    } else {
      errorsCount = 0;
    }
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
