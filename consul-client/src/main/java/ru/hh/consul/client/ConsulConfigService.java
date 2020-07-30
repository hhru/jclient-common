package ru.hh.consul.client;

import ru.hh.consul.client.model.ConsulUpstreamConfig;


public interface ConsulConfigService {
  ConsulUpstreamConfig getUpstreamConfig(String serviceName);
}
