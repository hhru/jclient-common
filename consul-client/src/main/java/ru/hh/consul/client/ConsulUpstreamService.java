package ru.hh.consul.client;

import ru.hh.consul.client.model.ConsulUpstream;

import java.util.Collection;

public interface ConsulUpstreamService {

  /**
   * Get upstreams by service name for local data center
   */
  Collection<ConsulUpstream> getUpstreams(String serviceName);

  /**
   * Get upstreams by service name for selected data centers
   */
  Collection<ConsulUpstream> getUpstreamsForDC(String serviceName, Collection<String> dc);
}
