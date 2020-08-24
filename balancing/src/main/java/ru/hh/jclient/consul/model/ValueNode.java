package ru.hh.jclient.consul.model;

import java.util.HashMap;
import java.util.Map;

public class ValueNode {
  private final boolean valueNode;
  private Map<String, ValueNode> map;
  private String value;

  public ValueNode(String value) {
    this.value = value;
    this.valueNode = true;
  }

  public ValueNode(Map<String, ValueNode> map) {
    this.map = map;
    this.valueNode = false;
  }

  public Map<String, ValueNode> getMap() {
    return map;
  }

  public String getValue() {
    checkTypeAndThrow(true);
    return value;
  }

  public void putValue(String key, String value) {
    checkTypeAndThrow(false);
    map.put(key, new ValueNode(value));
  }

  public ValueNode computeMapIfAbsent(String key) {
    checkTypeAndThrow(false);
    return map.computeIfAbsent(key, ignore -> new ValueNode(new HashMap<>()));
  }

  public ValueNode getOrDefault(String key, ValueNode valueNode) {
    checkTypeAndThrow(false);
    return map.getOrDefault(key, valueNode);
  }

  public ValueNode getNode(String key) {
    checkTypeAndThrow(false);
    return map.get(key);
  }

  public String getValue(String key) {
    checkTypeAndThrow(false);
    ValueNode valueNode = map.get(key);
    return valueNode != null ? valueNode.getValue() : null;
  }

  @Override
  public String toString() {
    return "ValueNode{" +
            "valueNode=" + valueNode +
            ", map=" + map +
            ", value='" + value + '\'' +
            '}';
  }

  private void checkTypeAndThrow(boolean flag) {
    if (!valueNode == flag) {
      throw new IllegalStateException("Unexpected action for node:" + this);
    }
  }
}
