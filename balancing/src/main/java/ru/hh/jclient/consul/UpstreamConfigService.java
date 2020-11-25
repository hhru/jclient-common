package ru.hh.jclient.consul;

import ru.hh.jclient.consul.model.ApplicationConfig;

import java.util.function.Consumer;


public interface UpstreamConfigService {
  ApplicationConfig getUpstreamConfig(String application);

  void setupListener(Consumer<String> callback);
}
