package ru.hh.jclient.consul;

import ru.hh.jclient.common.balancing.UpstreamConfig;
import ru.hh.jclient.consul.model.ValueNode;

import java.util.List;
import java.util.function.Consumer;


public interface ConsulConfigService {
  ValueNode getUpstreamConfig(String serviceName);

  UpstreamConfig getUpstreamConfig(String serviceName, String profile);

  void addListener(List<String> services, Consumer<String> callback);
}
