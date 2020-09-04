package ru.hh.jclient.common.balancing;

import static java.util.Objects.requireNonNull;
import ru.hh.jclient.consul.ValueNode;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class UpstreamConfig {
  public static final String DEFAULT = "default";
  public static final String PROFILE_NODE = "profile";
  static final int DEFAULT_MAX_TRIES = 2;
  static final int DEFAULT_MAX_FAILS = 1;
  static final int DEFAULT_MAX_TIMEOUT_TRIES = 1;

  static final int DEFAULT_FAIL_TIMEOUT_MS = 10;
  static final int DEFAULT_CONNECT_TIMEOUT_MS = 200;
  static final int DEFAULT_REQUEST_TIMEOUT_MS = 2_000;

  private int maxTries;
  private int maxFails;
  private int maxTimeoutTries;

  private int failTimeoutMs;
  private int connectTimeoutMs;
  private int requestTimeoutMs;

  private RetryPolicy retryPolicy = new RetryPolicy();

  public static UpstreamConfig fromTree(String serviceName, String profileName, String hostName, ValueNode rootNode) {

    ValueNode service = rootNode.getNode(serviceName);
    if (service == null) {
      return getDefaultConfig();
    }
    ValueNode host = service.getOrDefault(hostName, service.getNode(DEFAULT));
    if (host == null) {
      return getDefaultConfig();
    }
    ValueNode profiles = host.getNode(PROFILE_NODE);
    ValueNode configMap = profiles.getOrDefault(profileName, profiles.getNode(DEFAULT));
    try {
      UpstreamConfig upstreamConfig = new UpstreamConfig();
      upstreamConfig.maxTries = parseIntOrFallback(configMap.getValue("max_tries"), DEFAULT_MAX_TRIES);
      upstreamConfig.maxFails = parseIntOrFallback(configMap.getValue("max_fails"), DEFAULT_MAX_FAILS);
      upstreamConfig.maxTimeoutTries = parseIntOrFallback(configMap.getValue("max_timeout_tries"), DEFAULT_MAX_TIMEOUT_TRIES);
      upstreamConfig.failTimeoutMs = parseAndConvertToMillisOrFallback(configMap.getValue("fail_timeout_sec"), DEFAULT_FAIL_TIMEOUT_MS);
      upstreamConfig.connectTimeoutMs = parseAndConvertToMillisOrFallback(configMap.getValue("connect_timeout_sec"), DEFAULT_CONNECT_TIMEOUT_MS);
      upstreamConfig.requestTimeoutMs = parseAndConvertToMillisOrFallback(configMap.getValue("request_timeout_sec"), DEFAULT_REQUEST_TIMEOUT_MS);

      if (configMap.getValue("retry_policy") != null) {
        upstreamConfig.retryPolicy.update(configMap.getValue("retry_policy"));
      }

      return upstreamConfig;

    } catch (Exception e) {
      throw new UpstreamConfigFormatException("failed to get upstream config: " + rootNode, e);
    }
  }

  private static UpstreamConfig getDefaultConfig() {
    UpstreamConfig upstreamConfig = new UpstreamConfig();
    upstreamConfig.maxTries = DEFAULT_MAX_TRIES;
    upstreamConfig.maxFails = DEFAULT_MAX_FAILS;
    upstreamConfig.maxTimeoutTries = DEFAULT_MAX_TIMEOUT_TRIES;
    upstreamConfig.failTimeoutMs = DEFAULT_FAIL_TIMEOUT_MS;
    upstreamConfig.connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
    upstreamConfig.requestTimeoutMs = DEFAULT_REQUEST_TIMEOUT_MS;
    return upstreamConfig;
  }

  void update(UpstreamConfig newConfig) {
    requireNonNull(newConfig, "new config should not be empty");

    maxTries = newConfig.maxTries;
    maxFails = newConfig.maxFails;
    maxTimeoutTries = newConfig.maxTimeoutTries;
    failTimeoutMs = newConfig.failTimeoutMs;
    connectTimeoutMs = newConfig.connectTimeoutMs;
    requestTimeoutMs = newConfig.requestTimeoutMs;
    retryPolicy = newConfig.retryPolicy;

  }

  int getMaxTries() {
    return maxTries;
  }

  int getMaxFails() {
    return maxFails;
  }

  int getMaxTimeoutTries() {
    return maxTimeoutTries;
  }

  int getFailTimeoutMs() {
    return failTimeoutMs;
  }

  int getConnectTimeoutMs() {
    return connectTimeoutMs;
  }

  int getRequestTimeoutMs() {
    return requestTimeoutMs;
  }

  public RetryPolicy getRetryPolicy() {
    return retryPolicy;
  }

  @Override
  public String toString() {
    return "{max_tries=" + maxTries
        + ", max_timeout_tries=" + maxTimeoutTries
        + ", fail_timeout_ms=" + failTimeoutMs
        + ", max_fails=" + maxFails
        + ", connect_timeout_ms=" + connectTimeoutMs
        + ", request_timeout_ms=" + requestTimeoutMs
        + '}';
  }

  private static int parseIntOrFallback(String value, int defaultValue) {
    return Optional.ofNullable(value).map(Integer::parseInt).orElse(defaultValue);
  }

  private static int parseAndConvertToMillisOrFallback(String value, int defaultValue) {
    return Optional.ofNullable(value)
      .map(nonNullValue -> Math.round(Float.parseFloat(nonNullValue) * TimeUnit.SECONDS.toMillis(1)))
      .orElse(defaultValue);
  }

  private UpstreamConfig() {
  }
}
