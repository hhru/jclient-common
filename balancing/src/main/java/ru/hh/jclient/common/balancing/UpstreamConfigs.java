package ru.hh.jclient.common.balancing;

import java.util.Map;
import static java.util.Objects.requireNonNullElse;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT_CONFIG;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT_CONNECT_TIMEOUT_MS;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT_MAX_TIMEOUT_TRIES;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT_MAX_TRIES;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT_REQUEST_TIMEOUT_MS;

public final class UpstreamConfigs {
  private static final UpstreamConfigs DEFAULT_CONFIGS = new UpstreamConfigs(Map.of(DEFAULT, DEFAULT_CONFIG));

  private final Map<String, UpstreamConfig> configByProfile;

  private UpstreamConfigs(Map<String, UpstreamConfig> configByProfile) {
    this.configByProfile = configByProfile;
  }

  public static UpstreamConfigs of(Map<String, UpstreamConfig> configByProfile) {
    return new UpstreamConfigs(configByProfile);
  }

  public Optional<UpstreamConfig> get(String profile) {
    return Optional.ofNullable(configByProfile.get(profile));
  }

  public static UpstreamConfigs getDefaultConfig() {
    return DEFAULT_CONFIGS;
  }

  public static UpstreamConfig createUpstreamConfigWithDefaults(Integer maxTries, Integer maxTimeoutTries,
                                                                Float connectTimeoutMs, Float requestTimeoutMs,
                                                                Integer slowStartIntervalSec,
                                                                Map<Integer, Boolean> retryPolicyConfig) {
    UpstreamConfig upstreamConfig = new UpstreamConfig(
      requireNonNullElse(maxTries, DEFAULT_MAX_TRIES),
      requireNonNullElse(maxTimeoutTries, DEFAULT_MAX_TIMEOUT_TRIES),
      convertToMillisOrFallback(connectTimeoutMs, DEFAULT_CONNECT_TIMEOUT_MS),
      convertToMillisOrFallback(requestTimeoutMs, DEFAULT_REQUEST_TIMEOUT_MS)
    );
    upstreamConfig.getRetryPolicy().update(retryPolicyConfig);
    upstreamConfig.setSlowStartIntervalSec(requireNonNullElse(slowStartIntervalSec, 0));


    return upstreamConfig;
  }


  private static int convertToMillisOrFallback(Float value, int defaultValue) {
    return Optional.ofNullable(value)
      .map(nonNullValue -> Math.round(nonNullValue * TimeUnit.SECONDS.toMillis(1)))
      .orElse(defaultValue);
  }
}
