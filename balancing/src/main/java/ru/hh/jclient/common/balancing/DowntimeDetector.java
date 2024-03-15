package ru.hh.jclient.common.balancing;

public class DowntimeDetector {
  private static final float ERRORS_THRESHOLD = 0.2f;
  private final int n;
  private final int[] errors;
  private final float errorsThreshold;

  private volatile int errorsCount;
  private int current;

  public DowntimeDetector(int n) {
    this(n, ERRORS_THRESHOLD, 100);
  }

  public DowntimeDetector(int n, int initialLivePercent) {
    this(n, ERRORS_THRESHOLD, initialLivePercent);
  }

  public DowntimeDetector(int n, float errorsThreshold) {
    this(n, errorsThreshold, 100);
  }

  public DowntimeDetector(int n, float errorsThreshold, int initialLivePercent) {
    if (initialLivePercent < 0 || initialLivePercent > 100) {
      throw new IllegalArgumentException("invalid initial live percent value: " + initialLivePercent);
    }
    this.n = n;
    this.errors = new int[n];
    this.errorsThreshold = errorsThreshold;
    int initialErrorsCount = n * (100 - initialLivePercent) / 100;
    if (initialErrorsCount > 0) {
      this.errorsCount = initialErrorsCount;
      this.current = initialErrorsCount;
      for (int i = 0; i < initialErrorsCount; i++) {
        errors[i] = 1;
      }
    } else {
      this.errorsCount = 0;
      this.current = 0;
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

  public boolean tooMuchErrors() {
    return 1.0f * errorsCount / n > errorsThreshold;
  }

  public int successCount() {
    return n - errorsCount;
  }
}
