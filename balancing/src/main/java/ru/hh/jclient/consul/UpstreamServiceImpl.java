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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class UpstreamServiceImpl implements UpstreamService {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamServiceImpl.class);

  private final ServiceWeights defaultWeight;
  private final HealthClient healthClient;

  private final List<String> upstreamList;
  private final List<String> datacenterList;
  private final String currentDC;
  private final String currentNode;

  private final int watchSeconds;
  private final boolean allowCrossDC;
  private Consumer<String> callback;

  private final ConcurrentMap<String, CopyOnWriteArrayList<Server>> serverList = new ConcurrentHashMap<>();

  public UpstreamServiceImpl(List<String> upstreamList, List<String> datacenterList, Consul consulClient,
                             int watchSeconds, String currentDC, String currentNode, boolean allowCrossDC) {
    Preconditions.checkState(!upstreamList.isEmpty(), "UpstreamList can't be empty");
    Preconditions.checkState(!datacenterList.isEmpty(), "DatacenterList can't be empty");

    this.healthClient = consulClient.healthClient();
    this.datacenterList = datacenterList.stream().map(String::toLowerCase).collect(Collectors.toList());
    this.upstreamList = upstreamList;
    this.currentDC = currentDC.toLowerCase();
    this.currentNode = currentNode;
    this.allowCrossDC = allowCrossDC;
    this.watchSeconds = watchSeconds;
    this.defaultWeight = ImmutableServiceWeights.builder().passing(100).warning(10).build();

    if (!this.datacenterList.contains(this.currentDC)) {
      this.datacenterList.add(this.currentDC);
    }
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
    QueryOptions queryOptions = ImmutableQueryOptions.builder().datacenter(datacenter).build();
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
      return Collections.emptyList();
    }
    return servers;
  }

  void updateUpstreams(Map<ServiceHealthKey, ServiceHealth> upstreams, String serviceName, String datacenter) {
    Set<String> aliveServers = new HashSet<>();
    CopyOnWriteArrayList<Server> currentServers = serverList.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>());

    for (ServiceHealth serviceHealth : upstreams.values()) {
      String nodeName = serviceHealth.getNode().getNode();
      if (!isProd() && notSameNode(nodeName)) {
        continue;
      }

      Service service = serviceHealth.getService();

      List<HealthCheck> checks = serviceHealth.getChecks();
      boolean serviceFailed = checks.stream().anyMatch(check -> !check.getStatus().equals("passing"));

      if (serviceFailed) {
        continue;
      }

      String address = Server.addressFromHostPort(getAddress(serviceHealth), service.getPort());
      String nodeDatacenter = serviceHealth.getNode().getDatacenter().orElse(null);

      Server server = currentServers.stream()
        .filter(s -> address.equals(s.getAddress()))
        .findFirst().orElse(null);

      if (server == null) {
        server = new Server(address, service.getWeights().orElse(defaultWeight).getPassing(), nodeDatacenter);
        currentServers.add(server);
      }

      server.setMeta(service.getMeta());
      server.setTags(service.getTags());

      aliveServers.add(address);
    }

    disableDeadServices(currentServers, serviceName, datacenter, aliveServers);

    LOGGER.debug("upstreams for {} were updated in DC {}: {} ", serviceName, datacenter, currentServers);
  }

  private boolean isProd() {
    return Strings.isNullOrEmpty(currentNode);
  }

  private boolean notSameNode(String nodeName) {
    return !Strings.isNullOrEmpty(currentNode) && (!currentNode.equalsIgnoreCase(nodeName));
  }

  private static void disableDeadServices(CopyOnWriteArrayList<Server> servers, String serviceName, String datacenter, Set<String> aliveServers) {
    List<Server> deadServers = servers.stream()
      .filter(s -> datacenter.equals(s.getDatacenterLowerCased()))
      .filter(s -> !aliveServers.contains(s.getAddress()))
      .collect(Collectors.toList());

    LOGGER.debug("removing dead servers for {} in DC {}: {} ", serviceName, datacenter, deadServers);

    servers.removeAll(deadServers);
  }

  private static String getAddress(ServiceHealth serviceHealth) {
    String address = serviceHealth.getService().getAddress();
    if (!Strings.isNullOrEmpty(address)) {
      return address;
    }

    return serviceHealth.getNode().getAddress();
  }
}
