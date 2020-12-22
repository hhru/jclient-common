package ru.hh.jclient.common.balancing;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import ru.hh.jclient.consul.model.ApplicationConfig;
import ru.hh.jclient.consul.model.Host;
import ru.hh.jclient.consul.model.Profile;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class UpstreamConfig {
  public static final String DEFAULT = "default";
  public static final int DEFAULT_REQUEST_TIMEOUT_MS = 2_000;
  static final int DEFAULT_MAX_TRIES = 2;
  static final int DEFAULT_MAX_FAILS = 1;
  static final int DEFAULT_MAX_TIMEOUT_TRIES = 1;

  static final int DEFAULT_FAIL_TIMEOUT_MS = 10;
  static final int DEFAULT_CONNECT_TIMEOUT_MS = 200;
  private int maxTries;
  private int maxTimeoutTries;

  private int connectTimeoutMs;
  private int requestTimeoutMs;

  private RetryPolicy retryPolicy = new RetryPolicy();

  public static UpstreamConfig fromApplicationConfig(ApplicationConfig config, String hostName, String profileName) {
    if (config == null) {
      return getDefaultConfig();
    }
    Map<String, Host> hostMap = config.getHosts();
    if (hostMap == null || hostMap.get(hostName) == null) {
      return getDefaultConfig();
    }
    profileName = requireNonNullElse(profileName, DEFAULT);
    Map<String, Profile> profiles = hostMap.get(hostName).getProfiles();
    if (profiles == null || profiles.get(profileName) == null) {
      return getDefaultConfig();
    }
    Profile profile = profiles.get(profileName);

    try {
      UpstreamConfig upstreamConfig = new UpstreamConfig();
      upstreamConfig.maxTries = requireNonNullElse(profile.getMaxTries(), DEFAULT_MAX_TRIES);
      upstreamConfig.maxTimeoutTries = requireNonNullElse(profile.getMaxTimeoutTries(), DEFAULT_MAX_TIMEOUT_TRIES);
      upstreamConfig.connectTimeoutMs = convertToMillisOrFallback(profile.getConnectTimeoutMs(), DEFAULT_CONNECT_TIMEOUT_MS);
      upstreamConfig.requestTimeoutMs = convertToMillisOrFallback(profile.getRequestTimeoutMs(), DEFAULT_REQUEST_TIMEOUT_MS);
      upstreamConfig.retryPolicy.update(profile.getRetryPolicy());

      return upstreamConfig;

    } catch (Exception e) {
      throw new UpstreamConfigFormatException("failed to get upstream config: " + config, e);
    }
  }

  public static UpstreamConfig getDefaultConfig() {
    UpstreamConfig upstreamConfig = new UpstreamConfig();
    upstreamConfig.maxTries = DEFAULT_MAX_TRIES;
    upstreamConfig.maxTimeoutTries = DEFAULT_MAX_TIMEOUT_TRIES;
    upstreamConfig.connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
    upstreamConfig.requestTimeoutMs = DEFAULT_REQUEST_TIMEOUT_MS;
    return upstreamConfig;
  }

  void update(UpstreamConfig newConfig) {
    requireNonNull(newConfig, "new config should not be empty");

    maxTries = newConfig.maxTries;
    maxTimeoutTries = newConfig.maxTimeoutTries;
    connectTimeoutMs = newConfig.connectTimeoutMs;
    requestTimeoutMs = newConfig.requestTimeoutMs;
    retryPolicy = newConfig.retryPolicy;

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

  public RetryPolicy getRetryPolicy() {
    return retryPolicy;
  }

  @Override
  public String toString() {
    return "{max_tries=" + maxTries
        + ", max_timeout_tries=" + maxTimeoutTries
        + ", connect_timeout_ms=" + connectTimeoutMs
        + ", request_timeout_ms=" + requestTimeoutMs
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
