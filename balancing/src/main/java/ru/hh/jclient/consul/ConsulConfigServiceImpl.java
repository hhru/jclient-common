package ru.hh.jclient.consul;

import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.model.kv.Value;
import ru.hh.jclient.common.balancing.Upstream;
import ru.hh.jclient.common.balancing.UpstreamConfig;
import ru.hh.jclient.consul.model.ValueNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ConsulConfigServiceImpl implements ConsulConfigService {
  private static final String ROOT_PATH = "upstream";
  private final List<String> services = new ArrayList<>();
  private Consumer<String> callback;
  private volatile ValueNode cache = new ValueNode(ROOT_PATH);
  private final KeyValueClient kvClient;
  private Map<Upstream.UpstreamKey, UpstreamConfig> configMap;

  public ConsulConfigServiceImpl(Consul consulClient) {
    this.kvClient = consulClient.keyValueClient();
    initCache();
  }

  private ValueNode convertToTree(Collection<Value> values) {
    ValueNode rootNode = new ValueNode(new HashMap<>());
    for (Value value : values) {
      String key = value.getKey();
      String[] keys = key.split("/");
      ValueNode currentNode = rootNode;

      for (int keyPointer = 0; keyPointer < keys.length - 1; keyPointer++) {
        key = keys[keyPointer];
        currentNode = currentNode.computeMapIfAbsent(key);
      }
      currentNode.putValue(keys[keys.length - 1], value.getValueAsString().orElseGet(() -> ""));
    }
    return rootNode.getMap().get(ROOT_PATH);
  }

  private synchronized void updateCache(ValueNode map) {
    this.cache = map;
    for (Map.Entry<String, ValueNode> entry : map.getMap().entrySet()) {
      entry.getKey();
      entry.getValue();//host
      for (Map.Entry<String, ValueNode> entry2 : entry.getValue().getMap().entrySet()) {
        //////
      }

    }

    updateListeners();
  }

  private void updateListeners() {
    services.forEach(s -> callback.accept(s));
  }

  private void initCache() {
    KVCache cache = KVCache.newCache(kvClient, ROOT_PATH);
    cache.addListener(newValues -> {
      updateCache(convertToTree(newValues.values()));
    });
    cache.start();
  }

  //todo get service
  @Override
  public ValueNode getUpstreamConfig(String serviceName) {
    return cache;
  }

  @Override
  public UpstreamConfig getUpstreamConfig(String serviceName, String profile) {
    return configMap.get(new  Upstream.UpstreamKey(serviceName, profile));
  }

  @Override
  public void addListener(List<String> services, Consumer<String> callback) {
    this.services.addAll(services);
    this.callback = callback;
  }
}
