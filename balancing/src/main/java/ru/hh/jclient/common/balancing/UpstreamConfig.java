package ru.hh.jclient.common.balancing;

import java.util.Map;
import static java.util.Objects.requireNonNullElse;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import ru.hh.jclient.consul.model.ApplicationConfig;
import ru.hh.jclient.consul.model.Host;
import ru.hh.jclient.consul.model.Profile;

public final class UpstreamConfig {
  public static final String DEFAULT = "default";
  public static final int DEFAULT_REQUEST_TIMEOUT_MS = 2_000;
  static final int DEFAULT_MAX_TRIES = 2;
  static final int DEFAULT_MAX_TIMEOUT_TRIES = 1;

  static final int DEFAULT_CONNECT_TIMEOUT_MS = 200;
  private int maxTries;
  private int maxTimeoutTries;

  private int connectTimeoutMs;
  private int requestTimeoutMs;
  private int slowStartIntervalSec;

  private RetryPolicy retryPolicy = new RetryPolicy();

  public static Map<String, UpstreamConfig> fromApplicationConfig(ApplicationConfig config, String hostName) {
    if (config == null) {
      return getDefaultConfig();
    }
    Map<String, Host> hostMap = config.getHosts();
    if (hostMap == null || hostMap.get(hostName) == null) {
      return getDefaultConfig();
    }
    Map<String, Profile> profiles = hostMap.get(hostName).getProfiles();
    if (profiles == null || profiles.isEmpty()) {
      return getDefaultConfig();
    }
    try {
      Map<String, UpstreamConfig> configMap = profiles.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
          e -> convertProfileToUpstreamConfig(e.getValue())));
      return configMap;
    } catch (Exception e) {
      throw new UpstreamConfigFormatException("failed to get upstream config: " + config, e);
    }
  }

  private static UpstreamConfig convertProfileToUpstreamConfig(Profile profile) {
    UpstreamConfig upstreamConfig = new UpstreamConfig();
    upstreamConfig.maxTries = requireNonNullElse(profile.getMaxTries(), DEFAULT_MAX_TRIES);
    upstreamConfig.maxTimeoutTries = requireNonNullElse(profile.getMaxTimeoutTries(), DEFAULT_MAX_TIMEOUT_TRIES);
    upstreamConfig.connectTimeoutMs = convertToMillisOrFallback(profile.getConnectTimeoutMs(), DEFAULT_CONNECT_TIMEOUT_MS);
    upstreamConfig.requestTimeoutMs = convertToMillisOrFallback(profile.getRequestTimeoutMs(), DEFAULT_REQUEST_TIMEOUT_MS);
    upstreamConfig.slowStartIntervalSec = requireNonNullElse(profile.getSlowStartIntervalSec(), 0);
    upstreamConfig.retryPolicy.update(profile.getRetryPolicy());

    return upstreamConfig;
  }

  public static Map<String, UpstreamConfig> getDefaultConfig() {
    UpstreamConfig upstreamConfig = new UpstreamConfig();
    upstreamConfig.maxTries = DEFAULT_MAX_TRIES;
    upstreamConfig.maxTimeoutTries = DEFAULT_MAX_TIMEOUT_TRIES;
    upstreamConfig.connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
    upstreamConfig.requestTimeoutMs = DEFAULT_REQUEST_TIMEOUT_MS;
    return Map.of(DEFAULT, upstreamConfig);
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
