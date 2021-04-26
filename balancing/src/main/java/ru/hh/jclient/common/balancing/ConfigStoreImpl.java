package ru.hh.jclient.common.balancing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigStoreImpl implements ConfigStore {

  private final Map<String, UpstreamConfigs> configMap = new ConcurrentHashMap<>();

  @Override
  public UpstreamConfigs getUpstreamConfig(String upstream) {
    return configMap.get(upstream);
  }

  @Override
  public void updateConfig(String upstream, UpstreamConfigs upstreamConfigs) {
    configMap.put(upstream, upstreamConfigs);
  }
}
