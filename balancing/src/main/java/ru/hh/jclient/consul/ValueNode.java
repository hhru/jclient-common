package ru.hh.jclient.consul;

import java.util.HashMap;
import java.util.Map;

public class ValueNode {
  private final boolean valueNode;
  private Map<String, ValueNode> map;
  private String value;

  public ValueNode() {
    this.map = new HashMap<>();
    this.valueNode = false;
  }

  public ValueNode(String value) {
    this.value = value;
    this.valueNode = true;
  }

  Map<String, ValueNode> getMap() {
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

  public void putAll(Map<String, ValueNode> valueNodeMap) {
    checkTypeAndThrow(false);
    map.putAll(valueNodeMap);
  }

  public ValueNode computeMapIfAbsent(String key) {
    checkTypeAndThrow(false);
    return map.computeIfAbsent(key, ignore -> new ValueNode());
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
