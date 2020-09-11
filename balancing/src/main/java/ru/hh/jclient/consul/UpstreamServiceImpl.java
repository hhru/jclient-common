package ru.hh.jclient.consul;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.cache.ServiceHealthCache;
import com.orbitz.consul.cache.ServiceHealthKey;
import com.orbitz.consul.model.catalog.ImmutableServiceWeights;
import com.orbitz.consul.model.catalog.ServiceWeights;
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

  private final ServiceWeights defaultWeight;
  private final HealthClient healthClient;
  private final ScheduledExecutorService scheduledExecutor;

  private final List<String> upstreamList;
  private final List<String> datacenterList;
  private final String currentDC;
  private final String currentNode;

  private final int watchSeconds;
  private final boolean allowCrossDC;
  private Consumer<String> callback;

  private ConcurrentMap<String, List<Server>> serverList = new ConcurrentHashMap<>();
  private Map<String, ConcurrentMap<String, Server>> serverMap = new HashMap<>();

  public UpstreamServiceImpl(List<String> upstreamList, List<String> datacenterList, Consul consulClient, ScheduledExecutorService scheduledExecutor,
                             int watchSeconds, String currentDC, String currentNode, boolean allowCrossDC) {
    Preconditions.checkState(!upstreamList.isEmpty(), "UpstreamList can't be empty");
    Preconditions.checkState(!datacenterList.isEmpty(), "DatacenterList can't be empty");

    this.scheduledExecutor = scheduledExecutor;
    this.healthClient = consulClient.healthClient();
    this.datacenterList = datacenterList;
    this.upstreamList = upstreamList;
    this.currentDC = currentDC;
    this.currentNode = currentNode;
    this.allowCrossDC = allowCrossDC;
    this.watchSeconds = watchSeconds;
    this.defaultWeight = ImmutableServiceWeights.builder().passing(100).warning(10).build();
  }

  @Override
  public void setupListener(Consumer<String> callback) {
    this.callback = callback;
    upstreamList.forEach(s -> subscribeToUpstream(s, allowCrossDC));
  }

  void notifyListeners() {
    upstreamList.forEach(callback);
  }

  private void subscribeToUpstream(String serviceName, boolean allowCrossDC) {
    if (allowCrossDC) {
      for (String dataCenter : datacenterList) {
        initializeCache(serviceName, dataCenter);
      }
    } else {
      initializeCache(serviceName, currentDC);
    }
  }

  private void initializeCache(String serviceName, String datacenter) {
    QueryOptions queryOptions = ImmutableQueryOptions.builder().datacenter(currentDC).build();
    ServiceHealthCache svHealth = ServiceHealthCache.newCache(healthClient, serviceName, false, watchSeconds, queryOptions);

    LOGGER.debug("subscribe to service {}; dc {}", serviceName, datacenter);
    svHealth.addListener((Map<ServiceHealthKey, ServiceHealth> newValues) -> {
      updateUpstreams(newValues, serviceName, datacenter);
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
    Set<String> serversFromUpdate = new HashSet<>();
    Map<String, Server> storedServers = serverMap.computeIfAbsent(serviceName, k -> new ConcurrentHashMap<>());

    for (ServiceHealth serviceHealth : upstreams.values()) {
      if (notSameNode(serviceHealth.getNode().getNode())) {
        continue;
      }

      Service service = serviceHealth.getService();

      List<HealthCheck> checks = serviceHealth.getChecks();
      boolean serviceFailed = checks.stream().anyMatch(check -> !check.getStatus().equals("passing"));

      String address = Server.addressFromHostPort(service.getAddress(), service.getPort());
      Server server = storedServers.get(address);

      if (server != null) {
        if (serviceFailed && server.isActive()) {
          server.setAvailable(false, scheduledExecutor);
        }
      } else {
        String nodeDatacenter = serviceHealth.getNode().getDatacenter().orElse(null);

        server = new Server(address,
            service.getWeights().orElse(defaultWeight).getPassing(),
            nodeDatacenter);
        server.setAvailable(!serviceFailed, scheduledExecutor);
      }
      serversFromUpdate.add(address);
      storedServers.put(address, server);
    }

    disableDeregistredServices(storedServers, datacenter, serversFromUpdate);

    serverList.put(serviceName, List.copyOf(storedServers.values()));
    LOGGER.debug("upstreams for service: {} were updated; DC: {}; count :{} ", serviceName, datacenter, serversFromUpdate.size());
  }

  private boolean notSameNode(String nodeName) {
    return !Strings.isNullOrEmpty(currentNode) && !currentNode.equalsIgnoreCase(nodeName);
  }

  private void disableDeregistredServices(Map<String, Server> servers, String datacenter, Set<String> newServers) {
    servers.values().stream()
            .filter(s -> datacenter.equals(s.getDatacenter()))
            .filter(s -> !newServers.contains(s.getAddress()))
            .filter(Server::isActive).forEach(s -> s.setAvailable(false, scheduledExecutor));
  }
}
