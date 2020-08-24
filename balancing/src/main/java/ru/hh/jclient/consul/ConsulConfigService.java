package ru.hh.jclient.consul;

import ru.hh.jclient.consul.model.ValueNode;

import java.util.List;
import java.util.function.Consumer;


public interface ConsulConfigService {
  ValueNode getUpstreamConfig(String serviceName);

  void addListener(List<String> services, Consumer<String> callback);
}
