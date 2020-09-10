package ru.hh.jclient.consul;

import java.util.function.Consumer;


public interface UpstreamConfigService {
  ValueNode getUpstreamConfig();

  void setupListener(Consumer<String> callback);
}
