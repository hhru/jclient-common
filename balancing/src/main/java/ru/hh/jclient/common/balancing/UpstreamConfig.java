package ru.hh.jclient.common.balancing;

public final class UpstreamConfig {
  public static final String DEFAULT = "default";

  static final int DEFAULT_REQUEST_TIMEOUT_MS = 2_000;
  static final int DEFAULT_MAX_TRIES = 2;
  static final int DEFAULT_MAX_TIMEOUT_TRIES = 1;
  static final int DEFAULT_CONNECT_TIMEOUT_MS = 200;

  public static final UpstreamConfig DEFAULT_CONFIG = new UpstreamConfig(
    DEFAULT_MAX_TRIES, DEFAULT_MAX_TIMEOUT_TRIES,
    DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_REQUEST_TIMEOUT_MS
  );

  private final int maxTries;
  private final int maxTimeoutTries;
  private final int connectTimeoutMs;
  private final int requestTimeoutMs;
  private final RetryPolicy retryPolicy = new RetryPolicy();

  private int slowStartIntervalSec;
  private boolean isSessionRequired;

  UpstreamConfig(int maxTries, int maxTimeoutTries, int connectTimeoutMs, int requestTimeoutMs) {
    this.maxTries = maxTries;
    this.maxTimeoutTries = maxTimeoutTries;
    this.connectTimeoutMs = connectTimeoutMs;
    this.requestTimeoutMs = requestTimeoutMs;
  }

  public int getMaxTries() {
    return maxTries;
  }

  public int getMaxTimeoutTries() {
    return maxTimeoutTries;
  }

  public int getConnectTimeoutMs() {
    return connectTimeoutMs;
  }

  public int getRequestTimeoutMs() {
    return requestTimeoutMs;
  }

  public int getSlowStartIntervalSec() {
    return slowStartIntervalSec;
  }

  public boolean isSessionRequired() {
    return isSessionRequired;
  }

  public RetryPolicy getRetryPolicy() {
    return retryPolicy;
  }

  UpstreamConfig setSlowStartIntervalSec(int slowStartIntervalSec) {
    this.slowStartIntervalSec = slowStartIntervalSec;
    return this;
  }

  public UpstreamConfig setSessionRequired(boolean sessionRequired) {
    this.isSessionRequired = sessionRequired;
    return this;
  }

  @Override
  public String toString() {
    return "{max_tries=" + maxTries
      + ", max_timeout_tries=" + maxTimeoutTries
      + ", connect_timeout_ms=" + connectTimeoutMs
      + ", request_timeout_ms=" + requestTimeoutMs
      + ", slow_start_interval_sec=" + slowStartIntervalSec
      + ", is_session_required=" + isSessionRequired
      + '}';
  }
}
