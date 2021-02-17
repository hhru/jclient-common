package ru.hh.jclient.consul;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.hh.consul.Consul;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.cache.KVCache;
import ru.hh.consul.model.kv.Value;
import ru.hh.consul.option.ConsistencyMode;
import ru.hh.consul.option.ImmutableQueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.consul.model.ApplicationConfig;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class UpstreamConfigServiceImpl implements UpstreamConfigService {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamConfigServiceImpl.class);
  static final String ROOT_PATH = "upstream/";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private final ConsistencyMode consistencyMode;
  private final int watchSeconds;
  private final Set<String> services;
  private Consumer<String> callback;

  private final KeyValueClient kvClient;

  private final Map<String, ApplicationConfig> configMap = new HashMap<>();

  public UpstreamConfigServiceImpl(List<String> services, Consul consulClient, int watchSeconds, ConsistencyMode consistencyMode) {
    this(services, consulClient, watchSeconds, consistencyMode, true);
  }

  public UpstreamConfigServiceImpl(List<String> services, Consul consulClient, int watchSeconds, ConsistencyMode consistencyMode,
                                   boolean syncUpdate) {
    this.services = Set.copyOf(services);
    this.kvClient = consulClient.keyValueClient();
    this.watchSeconds = watchSeconds;
    this.consistencyMode = consistencyMode;
    if (syncUpdate) {
      LOGGER.debug("Trying to sync update configs");
      syncUpdateConfig();
    }
  }

  private void syncUpdateConfig() {
    List<Value> values = kvClient.getValues(ROOT_PATH, ImmutableQueryOptions.builder().consistencyMode(consistencyMode).build());
    if (values == null || values.isEmpty()) {
      throw new IllegalStateException("There's no upstreamConfigs in KV");
    }
    updateConfigs(values);
    Collection<String> unconfiguredServices = findUnconfiguredServices();
    if (!unconfiguredServices.isEmpty()) {
      throw new IllegalStateException("No valid configs found for services: " + unconfiguredServices);
    }
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

  void updateConfigs(Collection<Value> values) {
    for (Value value : values) {
      String key = value.getKey();
      String[] keys = key.split("/");
      if (keys.length != 2 || keys[1].isEmpty()) {
        LOGGER.trace("incorrect key: {} with value:{}", key, value.getValueAsString());
        continue;
      }
      if (!services.contains(keys[1])) {
        continue;
      }
      try {
        ApplicationConfig applicationConfig = OBJECT_MAPPER.readValue(value.getValueAsString().orElse(null), ApplicationConfig.class);
        configMap.put(keys[1], applicationConfig);
      } catch (IOException e) {
        LOGGER.error("Can't read value for key:{}", key, e);
      }
    }
  }

  private Collection<String> findUnconfiguredServices() {
    var absentConfigs = new HashSet<>(services);
    absentConfigs.removeAll(configMap.keySet());
    return absentConfigs;
  }

  void notifyListeners() {
    services.forEach(callback);
  }

  private void initConfigCache() {
    KVCache cache = KVCache.newCache(kvClient, ROOT_PATH, watchSeconds, ImmutableQueryOptions.builder().consistencyMode(consistencyMode).build());
    LOGGER.debug("subscribe to config:{}", ROOT_PATH);
    cache.addListener(newValues -> {
      LOGGER.debug("update config:{}", ROOT_PATH);
      updateConfigs(newValues.values());
      Collection<String> unconfiguredServices = findUnconfiguredServices();
      if (!unconfiguredServices.isEmpty()) {
        LOGGER.debug("No valid configs found for services: {}", unconfiguredServices);
      }
      LOGGER.debug("config updated. size {}", LOGGER.isDebugEnabled() ? configMap : configMap.size());
      notifyListeners();
    });
    cache.start();
  }
}
