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
import ru.hh.jclient.common.balancing.BalancingStrategyInitializer;
import ru.hh.jclient.common.balancing.ConfigStore;
import ru.hh.jclient.common.balancing.UpstreamConfig;
import ru.hh.jclient.common.balancing.UpstreamManager;
import ru.hh.jclient.common.balancing.config.ApplicationConfig;
import ru.hh.jclient.common.balancing.JClientInfrastructureConfig;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UpstreamConfigServiceImpl implements AutoCloseable, BalancingStrategyInitializer {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamConfigServiceImpl.class);
  static final String ROOT_PATH = "upstream/";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private final ConsistencyMode consistencyMode;
  private final int watchSeconds;
  private final String currentServiceName;
  private final Set<String> upstreamList;
  private final Collection<Consumer<Collection<String>>> callbacks;

  private final KeyValueClient kvClient;
  private final ConfigStore configStore;

  private volatile BigInteger initialIndex;
  private KVCache kvCache;

  public UpstreamConfigServiceImpl(JClientInfrastructureConfig infrastructureConfig, Consul consulClient,
                                   ConfigStore configStore, UpstreamManager upstreamManager,
                                   UpstreamConfigServiceConsulConfig config,
                                   Collection<Consumer<Collection<String>>> upstreamUpdateCallbacks) {
    this.upstreamList = Set.copyOf(config.getUpstreams());
    if (this.upstreamList == null || this.upstreamList.isEmpty()) {
      throw new IllegalArgumentException("UpstreamList can't be empty");
    }
    this.callbacks = Stream.of(
      upstreamUpdateCallbacks.stream(),
      Stream.of((Consumer<Collection<String>>)upstreamManager::updateUpstreams)
    )
      .flatMap(Function.identity())
      .collect(Collectors.toList());
    this.kvClient = consulClient.keyValueClient();
    this.configStore = configStore;
    this.watchSeconds = config.getWatchSeconds();
    this.currentServiceName = infrastructureConfig.getServiceName();
    this.consistencyMode = config.getConsistencyMode();
    if (config.isSyncUpdate()) {
      LOGGER.debug("Trying to sync update configs");
      syncUpdateConfig();
      checkAllUpstreamConfigsExist(true);
      callbacks.forEach(cb -> cb.accept(upstreamList));
    }
    initConfigCache();
  }

  private void syncUpdateConfig() {
    ImmutableQueryOptions options = ImmutableQueryOptions.builder()
      .caller(currentServiceName)
      .consistencyMode(consistencyMode).build();
    ConsulResponse<List<Value>> consulResponseWithValues = kvClient.getConsulResponseWithValues(ROOT_PATH, options);
    initialIndex = consulResponseWithValues.getIndex();
    List<Value> values = consulResponseWithValues.getResponse();
    if (values != null && !values.isEmpty()) {
      updateConfigs(values);
    }
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
        configStore.updateConfig(keys[1], ApplicationConfig.toUpstreamConfigs(applicationConfig, UpstreamConfig.DEFAULT));
      } catch (IOException e) {
        LOGGER.error("Can't read value for key:{}", key, e);
      }
    }
  }

  private void checkAllUpstreamConfigsExist(boolean throwIfError) {
    var absentConfigs = upstreamList.stream()
      .filter(upstream -> configStore.getUpstreamConfig(upstream) == null)
      .collect(Collectors.toSet());
    if (!absentConfigs.isEmpty()) {
      if (throwIfError) {
        throw new IllegalStateException("No valid configs found for services: " + absentConfigs);
      }
      LOGGER.warn("No valid configs found for services: {}", absentConfigs);
    }
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
      checkAllUpstreamConfigsExist(false);
      callbacks.forEach(cb -> cb.accept(upstreamList));
    });
    kvCache.start();
  }

  @Override
  public void close() {
    Optional.ofNullable(kvCache).ifPresent(KVCache::close);
  }
}
