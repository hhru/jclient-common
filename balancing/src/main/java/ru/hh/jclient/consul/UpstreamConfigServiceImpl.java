package ru.hh.jclient.consul;

import com.google.common.annotations.VisibleForTesting;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.model.kv.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class UpstreamConfigServiceImpl implements UpstreamConfigService {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamConfigServiceImpl.class);
  private static final String ROOT_PATH = "upstream";

  private final List<String> services;
  private Consumer<String> callback;
  private final KeyValueClient kvClient;
  private final int watchSeconds;

  private volatile ValueNode rootConfigNode = new ValueNode();

  public UpstreamConfigServiceImpl(List<String> services, Consul consulClient, int watchSeconds) {
    this.services = services;
    this.kvClient = consulClient.keyValueClient();
    this.watchSeconds = watchSeconds;
  }

  @Override
  public ValueNode getUpstreamConfig() {
    return rootConfigNode;
  }

  @Override
  public void setupListener(Consumer<String> callback) {
    this.callback = callback;
    initConfigCache();
  }

  ValueNode convertToTree(Collection<Value> values) {
    ValueNode rootNode = new ValueNode();
    for (Value value : values) {
      String key = value.getKey();
      String[] keys = key.split("/");
      if (keys.length < 4) {
        LOGGER.info("short path: {} for value:{}; Will be skipped", key, value.getValueAsString());
        continue;
      }
      ValueNode currentNode = rootNode;

      for (int keyPointer = 0; keyPointer < keys.length - 1; keyPointer++) {
        key = keys[keyPointer];
        currentNode = currentNode.computeMapIfAbsent(key);
      }
      currentNode.putValue(keys[keys.length - 1], value.getValueAsString().orElseGet(() -> ""));
    }
    ValueNode resultRootNode = rootNode.getNode(ROOT_PATH);
    return Objects.requireNonNullElse(resultRootNode, new ValueNode());
  }

  private void updateCache(ValueNode map) {
    this.rootConfigNode = map;
  }

  @VisibleForTesting
  void notifyListeners() {
    services.forEach(callback);
  }

  private void initConfigCache() {
    KVCache cache = KVCache.newCache(kvClient, ROOT_PATH, watchSeconds);
    LOGGER.debug("subscribe to config:{}", ROOT_PATH);
    cache.addListener(newValues -> {
      LOGGER.debug("update config:{}", ROOT_PATH);
      updateCache(convertToTree(newValues.values()));
      LOGGER.debug("new config:{}", rootConfigNode);
      notifyListeners();
    });
    cache.start();
  }
}
