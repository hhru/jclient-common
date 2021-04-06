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
import ru.hh.jclient.consul.model.config.JClientInfrastructureConfig;
import ru.hh.jclient.consul.model.config.UpstreamConfigServiceConsulConfig;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Optional.ofNullable;

public class UpstreamConfigServiceImpl implements UpstreamConfigService {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamConfigServiceImpl.class);
  static final String ROOT_PATH = "upstream/";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private final ConsistencyMode consistencyMode;
  private final int watchSeconds;
  private final String currentServiceName;
  private final Set<String> upstreamList;
  private Consumer<String> callback;

  private final KeyValueClient kvClient;

  private final Map<String, ApplicationConfig> configMap = new HashMap<>();

  public UpstreamConfigServiceImpl(JClientInfrastructureConfig infrastructureConfig, Consul consulClient, UpstreamConfigServiceConsulConfig config) {
    this(List.of(), infrastructureConfig.getServiceName(), consulClient, config);
  }

  /**
   * use upstream list parameter in {@link UpstreamConfigServiceConsulConfig}
   */
  @Deprecated(forRemoval = true)
  public UpstreamConfigServiceImpl(List<String> upstreamList, String currentServiceName, Consul consulClient,
                                   UpstreamConfigServiceConsulConfig config) {
    this.upstreamList = Set.copyOf(ofNullable(upstreamList).filter(Predicate.not(Collection::isEmpty)).orElseGet(config::getUpstreams));
    if (this.upstreamList == null || this.upstreamList.isEmpty()) {
      throw new IllegalArgumentException("UpstreamList can't be empty");
    }
    this.kvClient = consulClient.keyValueClient();
    this.watchSeconds = config.getWatchSeconds();
    this.currentServiceName = currentServiceName;
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
    List<Value> values = kvClient.getValues(ROOT_PATH, options);
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
    KVCache cache = KVCache.newCache(kvClient, ROOT_PATH, watchSeconds, queryOptions);
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
