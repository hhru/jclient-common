package ru.hh.jclient.consul;

import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.model.kv.Value;
import ru.hh.jclient.consul.model.ValueNode;

import java.util.Collection;
import java.util.HashMap;

public class ConsulConfigServiceImpl implements ConsulConfigService {
  private static final String ROOT_PATH = "upstream";
  private volatile ValueNode cache = new ValueNode(ROOT_PATH);
  private final KeyValueClient kvClient;

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

  @Override
  public ValueNode getUpstreamConfig(String serviceName, String profile) {
    return cache;
  }

  private synchronized void updateCache(ValueNode map) {
    this.cache = map;
  }


  private void initCache() {
    KVCache cache = KVCache.newCache(kvClient, ROOT_PATH);
    cache.addListener(newValues -> {
      updateCache(convertToTree(newValues.values()));
    });
    cache.start();
  }
}
