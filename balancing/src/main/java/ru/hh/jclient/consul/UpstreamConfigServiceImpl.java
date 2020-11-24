package ru.hh.jclient.consul;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.option.ConsistencyMode;
import com.orbitz.consul.option.ImmutableQueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.consul.model.config.ApplicationConfig;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class UpstreamConfigServiceImpl implements UpstreamConfigService {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamConfigServiceImpl.class);
  private static final String ROOT_PATH = "upstream/";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private final ConsistencyMode consistencyMode;
  private final int watchSeconds;
  private final List<String> services;
  private Consumer<String> callback;

  private final KeyValueClient kvClient;

  private final Map<String, ApplicationConfig> configMap = new HashMap<>();

  public UpstreamConfigServiceImpl(List<String> services, Consul consulClient, int watchSeconds, ConsistencyMode consistencyMode) {
    this.services = services;
    this.kvClient = consulClient.keyValueClient();
    this.watchSeconds = watchSeconds;
    this.consistencyMode = consistencyMode;
  }

  @Override
  public ApplicationConfig getUpstreamConfig(String application) {
    return configMap.get(application);
  }

  @Override
  public void setupListener(Consumer<String> callback) {
    this.callback = callback;
    initConfigCache();
  }

  Map<String, ApplicationConfig> readValues(Collection<Value> values) {
    for (Value value : values) {
      String key = value.getKey();
      String[] keys = key.split("/");
      if (keys.length != 2) {
        LOGGER.debug("incorrect key: {} with value:{}; Will be skipped", key, value.getValueAsString());
        continue;
      }
      try {
        ApplicationConfig applicationConfig;

        applicationConfig = OBJECT_MAPPER.readValue(value.getValueAsString().orElse(null), ApplicationConfig.class);
        configMap.put(keys[1], applicationConfig);
      } catch (IOException e) {
        LOGGER.error("Can't read value for key:{}", key, e);
        throw new RuntimeException("Can't read value for key:" + key, e);
      }
    }
    return configMap;
  }

  void notifyListeners() {
    services.forEach(callback);
  }

  private void initConfigCache() {
    KVCache cache = KVCache.newCache(kvClient, ROOT_PATH, watchSeconds, ImmutableQueryOptions.builder().consistencyMode(consistencyMode).build());
    LOGGER.debug("subscribe to config:{}", ROOT_PATH);
    cache.addListener(newValues -> {
      LOGGER.debug("update config:{}", ROOT_PATH);
      readValues(newValues.values());
      LOGGER.info("config updated. size {}", configMap.size());
      LOGGER.debug("new config:{}", configMap);
      notifyListeners();
    });
    cache.start();
  }
}
