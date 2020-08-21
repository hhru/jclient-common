package ru.hh.jclient.consul;

import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.cache.ServiceHealthCache;
import com.orbitz.consul.cache.ServiceHealthKey;
import com.orbitz.consul.model.health.HealthCheck;
import com.orbitz.consul.model.health.Service;
import com.orbitz.consul.model.health.ServiceHealth;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.orbitz.consul.option.QueryOptions;
import ru.hh.jclient.common.balancing.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsulUpstreamServiceImpl implements ConsulUpstreamService {
  private final HealthClient healthClient;
  private final List<String> upstreamList;
  private final List<String> datacenterList;
  private static Map<String, Map<String, Server>> listMap = new HashMap<>(); //todo и внутреннюю  канкарент
  private final int watchSeconds = 10;

  public ConsulUpstreamServiceImpl(List<String> upstreamList, List<String> datacenterList, boolean allowCrossDC, Consul consulClient) {
    //todo check not empty params
    this.upstreamList = upstreamList;
    this.datacenterList = datacenterList;
    this.healthClient = consulClient.healthClient();
    upstreamList.forEach(s -> registerUpstream(s, allowCrossDC)); //todo postConstruct?
  }

  @Override
  public void registerUpstream(String serviceName, boolean allowCrossDC) {
    if (allowCrossDC) {
      for (String dataCenter : datacenterList) {
        QueryOptions queryOptions = ImmutableQueryOptions.builder().datacenter(dataCenter).build();
        initializeCache(serviceName, queryOptions);
      }
    } else {
      initializeCache(serviceName, QueryOptions.BLANK);
    }

  }

  private void initializeCache(String serviceName, QueryOptions queryOptions) {
    ServiceHealthCache svHealth = ServiceHealthCache.newCache(healthClient, serviceName, false, watchSeconds, queryOptions);
    svHealth.addListener((Map<ServiceHealthKey, ServiceHealth> newValues) -> {
      updateUpstreams(newValues, serviceName);
    });
    svHealth.start();
  }

  @Override
  public List<Server> getServers(String serviceName) {
    Map<String, Server> serverServerMap = listMap.get(serviceName);
    //todo check not null
    return new ArrayList<>(serverServerMap.values()); //todo remove list
  }

  private static void updateUpstreams(Map<ServiceHealthKey, ServiceHealth> upstreams, String serviceName) {
    Map<String, Server> servers = listMap.computeIfAbsent(serviceName, k -> new HashMap<>());

    for (ServiceHealth serviceHealth : upstreams.values()) {
      Service service = serviceHealth.getService();

      Server s = servers.get(service.getAddress());
      List<HealthCheck> checks = serviceHealth.getChecks();
      if (s != null) {
        //todo check deregistred
        boolean isServiceFailed = checks.stream().anyMatch(check -> !check.getStatus().equals("passing"));
        if (isServiceFailed && s.isActive()) {
          s.setAvailable(false);
        }
      } else {
        servers.put(service.getAddress(),
                new Server(service.getAddress(), 1, "null", serviceHealth.getNode().getDatacenter().get())); //todo weight, rack);
      }
    }
  }
}
