package ru.hh.jclient.common.balancing;

import ru.hh.jclient.common.balancing.config.ApplicationConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigStoreImpl implements ConfigStore {

  private final Map<String, ApplicationConfig> configMap = new ConcurrentHashMap<>();

  @Override
  public ApplicationConfig getUpstreamConfig(String upstream) {
    return configMap.get(upstream);
  }

  @Override
  public void updateConfig(String upstream, ApplicationConfig applicationConfig) {
    configMap.put(upstream, applicationConfig);
  }
}
