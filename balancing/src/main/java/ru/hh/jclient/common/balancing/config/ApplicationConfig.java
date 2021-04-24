package ru.hh.jclient.common.balancing.config;

import ru.hh.jclient.common.balancing.UpstreamConfig;
import ru.hh.jclient.common.balancing.UpstreamConfigFormatException;

import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

public class ApplicationConfig {
  private Map<String, Host> hosts;

  public Map<String, Host> getHosts() {
    return hosts;
  }

  public ApplicationConfig setHosts(Map<String, Host> hosts) {
    this.hosts = hosts;
    return this;
  }

  public static Map<String, UpstreamConfig> toUpstreamConfigs(ApplicationConfig config, String hostName) {
    if (config == null) {
      return UpstreamConfig.getDefaultConfig();
    }
    Map<String, Host> hostMap = config.getHosts();
    if (hostMap == null || hostMap.get(hostName) == null) {
      return UpstreamConfig.getDefaultConfig();
    }
    Map<String, Profile> profiles = hostMap.get(hostName).getProfiles();
    if (profiles == null || profiles.isEmpty()) {
      return UpstreamConfig.getDefaultConfig();
    }
    try {
      return profiles.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> convertProfileToUpstreamConfig(e.getValue())));
    } catch (Exception e) {
      throw new UpstreamConfigFormatException("failed to get upstream config: " + config, e);
    }
  }

  private static UpstreamConfig convertProfileToUpstreamConfig(Profile profile) {
    return UpstreamConfig.create(
      profile.getMaxTries(), profile.getMaxTimeoutTries(),
      profile.getConnectTimeoutMs(), profile.getRequestTimeoutMs(),
      profile.getSlowStartIntervalSec(),
      Optional.ofNullable(profile.getRetryPolicy())
        .map(policy -> policy.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().isIdempotent())))
        .orElseGet(Map::of)
    );
  }

  @Override
  public String toString() {
    return "ApplicationConfig{" +
        "hosts=" + hosts +
        '}';
  }
}
