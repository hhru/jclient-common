package ru.hh.jclient.common.balancing;

import jakarta.annotation.Nullable;
import java.util.Map;
import static java.util.Objects.requireNonNullElse;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT_CONFIG;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT_CONNECT_TIMEOUT_MS;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT_MAX_TIMEOUT_TRIES;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT_MAX_TRIES;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT_REQUEST_TIMEOUT_MS;
import ru.hh.jclient.common.balancing.config.BalancingStrategyType;

public final class UpstreamConfigs {
  private static final Logger log = LoggerFactory.getLogger(UpstreamConfigs.class);

  private static final BalancingStrategyType DEFAULT_BALANCING_STRATEGY = BalancingStrategyType.WEIGHTED;

  private final Map<String, UpstreamConfig> configByProfile;
  private final BalancingStrategyType balancingStrategyType;

  private UpstreamConfigs(Map<String, UpstreamConfig> configByProfile, @Nullable String balancingStrategyType) {
    this.configByProfile = configByProfile;

    BalancingStrategyType parsedStrategy = BalancingStrategyType.tryParseFromString(balancingStrategyType);
    if (parsedStrategy == null && balancingStrategyType != null) {
      log.error("Invalid balancing strategy '{}', will use default ('{}')", balancingStrategyType, DEFAULT_BALANCING_STRATEGY.getPublicName());
    }

    this.balancingStrategyType = requireNonNullElse(parsedStrategy, DEFAULT_BALANCING_STRATEGY);
  }

  public static UpstreamConfigs of(Map<String, UpstreamConfig> configByProfile, @Nullable String balancingStrategyType) {
    return new UpstreamConfigs(configByProfile, balancingStrategyType);
  }

  public Optional<UpstreamConfig> get(String profile) {
    return Optional.ofNullable(configByProfile.get(profile));
  }

  public BalancingStrategyType getBalancingStrategyType() {
    return balancingStrategyType;
  }

  public static UpstreamConfigs getDefaultConfig() {
    return getDefaultConfig(null);
  }

  public static UpstreamConfigs getDefaultConfig(@Nullable String balancingStrategyType) {
    return new UpstreamConfigs(Map.of(DEFAULT, DEFAULT_CONFIG), balancingStrategyType);
  }

  public static UpstreamConfig createUpstreamConfigWithDefaults(
      Integer maxTries,
      Integer maxTimeoutTries,
      Float connectTimeoutSec,
      Float requestTimeoutSec,
      Integer slowStartIntervalSec,
      Boolean isSessionRequired,
      Map<Integer, Boolean> retryPolicyConfig
  ) {
    UpstreamConfig upstreamConfig = new UpstreamConfig(
        requireNonNullElse(maxTries, DEFAULT_MAX_TRIES),
        requireNonNullElse(maxTimeoutTries, DEFAULT_MAX_TIMEOUT_TRIES),
        convertToMillisOrFallback(connectTimeoutSec, DEFAULT_CONNECT_TIMEOUT_MS),
        convertToMillisOrFallback(requestTimeoutSec, DEFAULT_REQUEST_TIMEOUT_MS)
    );
    upstreamConfig.getRetryPolicy().update(retryPolicyConfig);
    upstreamConfig.setSlowStartIntervalSec(requireNonNullElse(slowStartIntervalSec, 0));
    upstreamConfig.setSessionRequired(requireNonNullElse(isSessionRequired, false));

    return upstreamConfig;
  }


  private static int convertToMillisOrFallback(Float value, int defaultValue) {
    return Optional
        .ofNullable(value)
        .map(nonNullValue -> Math.round(nonNullValue * TimeUnit.SECONDS.toMillis(1)))
        .orElse(defaultValue);
  }

  @Override
  public String toString() {
    return "UpstreamConfigs{" +
        "configByProfile=" + configByProfile +
        ", balancingStrategyType=" + balancingStrategyType +
        '}';
  }
}
