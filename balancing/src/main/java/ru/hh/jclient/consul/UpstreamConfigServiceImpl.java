package ru.hh.jclient.consul;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.hh.consul.Consul;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.cache.KVCache;
import ru.hh.consul.model.ConsulResponse;
import ru.hh.consul.model.kv.Value;
import ru.hh.consul.option.ConsistencyMode;
import ru.hh.consul.option.ImmutableQueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.consul.model.ApplicationConfig;
import ru.hh.jclient.consul.model.config.JClientInfrastructureConfig;
import ru.hh.jclient.consul.model.config.UpstreamConfigServiceConsulConfig;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class UpstreamConfigServiceImpl implements UpstreamConfigService, AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamConfigServiceImpl.class);
  static final String ROOT_PATH = "upstream/";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private final ConsistencyMode consistencyMode;
  private final int watchSeconds;
  private final String currentServiceName;
  private final Set<String> upstreamList;
  private Consumer<String> callback;

  private final KeyValueClient kvClient;

  private volatile BigInteger initialIndex;
  private KVCache kvCache;

  private final Map<String, ApplicationConfig> configMap = new HashMap<>();

  public UpstreamConfigServiceImpl(JClientInfrastructureConfig infrastructureConfig, Consul consulClient, UpstreamConfigServiceConsulConfig config) {
    this.upstreamList = Set.copyOf(config.getUpstreams());
    if (this.upstreamList == null || this.upstreamList.isEmpty()) {
      throw new IllegalArgumentException("UpstreamList can't be empty");
    }
    this.kvClient = consulClient.keyValueClient();
    this.watchSeconds = config.getWatchSeconds();
    this.currentServiceName = infrastructureConfig.getServiceName();
    this.consistencyMode = config.getConsistencyMode();
    if (config.isSyncUpdate()) {
      LOGGER.debug("Trying to sync update configs");
      syncUpdateConfig();
    }
  }

  private void syncUpdateConfig() {
    ImmutableQueryOptions options = ImmutableQueryOptions.builder()
      .caller(currentServiceName)
      .consistencyMode(consistencyMode).build();
    ConsulResponse<List<Value>> consulResponseWithValues = kvClient.getConsulResponseWithValues(ROOT_PATH, options);
    initialIndex = consulResponseWithValues.getIndex();
    List<Value> values = consulResponseWithValues.getResponse();
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

  private void updateConfigs(Collection<Value> values) {
    for (Value value : values) {
      String key = value.getKey();
      String[] keys = key.split("/");
      if (keys.length != 2 || keys[1].isEmpty()) {
        LOGGER.trace("incorrect key: {} with value:{}", key, value.getValueAsString());
        continue;
      }
      if (!upstreamList.contains(keys[1])) {
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
    var absentConfigs = new HashSet<>(upstreamList);
    absentConfigs.removeAll(configMap.keySet());
    return absentConfigs;
  }

  void notifyListeners() {
    upstreamList.forEach(callback);
  }

  private void initConfigCache() {
    ImmutableQueryOptions queryOptions = ImmutableQueryOptions.builder()
      .caller(currentServiceName)
      .consistencyMode(consistencyMode)
      .build();
    kvCache = KVCache.newCache(kvClient, ROOT_PATH, watchSeconds, initialIndex, queryOptions);
    LOGGER.debug("subscribe to config:{}", ROOT_PATH);
    kvCache.addListener(newValues -> {
      LOGGER.debug("update config:{}", ROOT_PATH);
      updateConfigs(newValues.values());
      Collection<String> unconfiguredServices = findUnconfiguredServices();
      if (!unconfiguredServices.isEmpty()) {
        LOGGER.debug("No valid configs found for services: {}", unconfiguredServices);
      }
      LOGGER.debug("config updated. size {}", LOGGER.isDebugEnabled() ? configMap : configMap.size());
      notifyListeners();
    });
    kvCache.start();
  }

  @Override
  public void close() throws Exception {
    Optional.ofNullable(kvCache).ifPresent(KVCache::close);
  }
}
