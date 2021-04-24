package ru.hh.jclient.common.balancing;

import java.util.Map;
import static java.util.Objects.requireNonNullElse;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class UpstreamConfig {
  public static final String DEFAULT = "default";
  public static final UpstreamConfig DEFAULT_CONFIG = buildDefaultConfig();
  private static final Map<String, UpstreamConfig> DEFAULT_CONFIGS = Map.of(DEFAULT, DEFAULT_CONFIG);
  static final int DEFAULT_REQUEST_TIMEOUT_MS = 2_000;
  static final int DEFAULT_MAX_TRIES = 2;
  static final int DEFAULT_MAX_TIMEOUT_TRIES = 1;

  static final int DEFAULT_CONNECT_TIMEOUT_MS = 200;
  private int maxTries;
  private int maxTimeoutTries;

  private int connectTimeoutMs;
  private int requestTimeoutMs;
  private int slowStartIntervalSec;

  private final RetryPolicy retryPolicy = new RetryPolicy();

  public static Map<String, UpstreamConfig> getDefaultConfig() {
    return DEFAULT_CONFIGS;
  }

  public static UpstreamConfig create(Integer maxTries, Integer maxTimeoutTries,
                                      Float connectTimeoutMs, Float requestTimeoutMs,
                                      Integer slowStartIntervalSec,
                                      Map<Integer, Boolean> retryPolicyConfig) {
    UpstreamConfig upstreamConfig = new UpstreamConfig();
    upstreamConfig.maxTries = requireNonNullElse(maxTries, DEFAULT_MAX_TRIES);
    upstreamConfig.maxTimeoutTries = requireNonNullElse(maxTimeoutTries, DEFAULT_MAX_TIMEOUT_TRIES);
    upstreamConfig.connectTimeoutMs = convertToMillisOrFallback(connectTimeoutMs, DEFAULT_CONNECT_TIMEOUT_MS);
    upstreamConfig.requestTimeoutMs = convertToMillisOrFallback(requestTimeoutMs, DEFAULT_REQUEST_TIMEOUT_MS);
    upstreamConfig.slowStartIntervalSec = requireNonNullElse(slowStartIntervalSec, 0);
    upstreamConfig.retryPolicy.update(retryPolicyConfig);

    return upstreamConfig;
  }

  private static UpstreamConfig buildDefaultConfig() {
    UpstreamConfig upstreamConfig = new UpstreamConfig();
    upstreamConfig.maxTries = DEFAULT_MAX_TRIES;
    upstreamConfig.maxTimeoutTries = DEFAULT_MAX_TIMEOUT_TRIES;
    upstreamConfig.connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
    upstreamConfig.requestTimeoutMs = DEFAULT_REQUEST_TIMEOUT_MS;
    return upstreamConfig;
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

  public RetryPolicy getRetryPolicy() {
    return retryPolicy;
  }

  @Override
  public String toString() {
    return "{max_tries=" + maxTries
        + ", max_timeout_tries=" + maxTimeoutTries
        + ", connect_timeout_ms=" + connectTimeoutMs
        + ", request_timeout_ms=" + requestTimeoutMs
        + ", slow_start_interval_sec=" + slowStartIntervalSec
        + '}';
  }

  private static int convertToMillisOrFallback(Float value, int defaultValue) {
    return Optional.ofNullable(value)
      .map(nonNullValue -> Math.round(nonNullValue * TimeUnit.SECONDS.toMillis(1)))
      .orElse(defaultValue);
  }

  private UpstreamConfig() {
  }
}
