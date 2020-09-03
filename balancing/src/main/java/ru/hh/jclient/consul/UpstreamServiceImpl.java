package ru.hh.jclient.consul;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.cache.ServiceHealthCache;
import com.orbitz.consul.cache.ServiceHealthKey;
import com.orbitz.consul.model.health.HealthCheck;
import com.orbitz.consul.model.health.Service;
import com.orbitz.consul.model.health.ServiceHealth;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.orbitz.consul.option.QueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.balancing.Server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class UpstreamServiceImpl implements UpstreamService {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamServiceImpl.class);
  private final HealthClient healthClient;
  private final List<String> datacenterList;
  private final ScheduledExecutorService scheduledExecutor;
  private Map<String, ConcurrentMap<String, Server>> serverMap = new HashMap<>();
  private ConcurrentMap<String, List<Server>> serverList = new ConcurrentHashMap<>();

  private final int watchSeconds;
  private final String datacenter;
  private final boolean allowCrossDC;

  private final List<String> services;
  private Consumer<String> callback;

  public UpstreamServiceImpl(List<String> upstreamList, List<String> datacenterList, Consul consulClient, ScheduledExecutorService scheduledExecutor,
                             int watchSeconds, String datacenter, boolean allowCrossDC) {
    Preconditions.checkState(!upstreamList.isEmpty(), "UpstreamList can't be empty");
    Preconditions.checkState(!datacenterList.isEmpty(), "DatacenterList can't be empty");

    this.datacenterList = datacenterList;
    this.scheduledExecutor = scheduledExecutor;
    this.healthClient = consulClient.healthClient();
    this.watchSeconds = watchSeconds;
    this.services = upstreamList;
    this.datacenter = datacenter;
    this.allowCrossDC = allowCrossDC;
  }

  @Override
  public void setupListener(Consumer<String> callback) {
    setListener(callback);
    services.forEach(s -> subscribeToUpstream(s, allowCrossDC));
  }

  @VisibleForTesting
  public void setListener(Consumer<String> callback) {
    this.callback = callback;
  }

  void notifyListeners() {
    services.forEach(callback);
  }

  private void subscribeToUpstream(String serviceName, boolean allowCrossDC) {
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
    String dc = queryOptions.getDatacenter().orElse(datacenter);
    LOGGER.debug("subscribe to service {}; dc {}", serviceName, dc);
    svHealth.addListener((Map<ServiceHealthKey, ServiceHealth> newValues) -> {
      updateUpstreams(newValues, serviceName, dc);
      notifyListeners();
    });
    svHealth.start();
  }

  @Override
  public List<Server> getServers(String serviceName) {
    List<Server> servers = serverList.get(serviceName);
    if (servers == null) {
      throw new IllegalStateException("Empty server list for service: " + serviceName);
    }
    return servers;
  }

  void updateUpstreams(Map<ServiceHealthKey, ServiceHealth> upstreams, String serviceName, String datacenter) {
    Set<String> newServers = new HashSet<>();
    Map<String, Server> servers = serverMap.computeIfAbsent(serviceName, k -> new ConcurrentHashMap<>());

    for (ServiceHealth serviceHealth : upstreams.values()) {
      Service service = serviceHealth.getService();
      String address = Server.addressFromHostPort(service.getAddress(), service.getPort());
      newServers.add(address);
      Server server = servers.get(address);
      List<HealthCheck> checks = serviceHealth.getChecks();
      String nodeDatacenter = serviceHealth.getNode().getDatacenter().orElse(null);
      boolean serviceFailed = checks.stream().anyMatch(check -> !check.getStatus().equals("passing"));
      if (server != null) {
        if (serviceFailed && server.isActive()) {
          server.setAvailable(false, scheduledExecutor);
        }
      } else {
        server = new Server(address,
                service.getWeights().get().getPassing(),
                nodeDatacenter);
        server.setAvailable(!serviceFailed, scheduledExecutor);
      }
      servers.put(address, server);
    }
    serverList.put(serviceName, List.copyOf(servers.values()));
    LOGGER.debug("upstreams for service: {} were updated; DC: {}; count :{} ", serviceName, datacenter, newServers.size());
    disableDeregistredServices(servers, datacenter, newServers);
  }


  private void disableDeregistredServices(Map<String, Server> servers, String datacenter, Set<String> newServers) {
    //todo передавать датацентр из конфигов
    servers.values().stream()
            .filter(s -> datacenter.equals(s.getDatacenter()))
            .filter(s -> !newServers.contains(s.getAddress()))
            .filter(Server::isActive).forEach(s -> s.setAvailable(false, scheduledExecutor));
  }
}
