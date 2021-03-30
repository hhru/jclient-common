package ru.hh.jclient.consul;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import ru.hh.consul.Consul;
import ru.hh.consul.HealthClient;
import ru.hh.consul.cache.ServiceHealthCache;
import ru.hh.consul.cache.ServiceHealthKey;
import ru.hh.consul.model.catalog.ImmutableServiceWeights;
import ru.hh.consul.model.catalog.ServiceWeights;
import ru.hh.consul.model.health.Service;
import ru.hh.consul.model.health.ServiceHealth;
import ru.hh.consul.option.ConsistencyMode;
import ru.hh.consul.option.ImmutableQueryOptions;
import ru.hh.consul.option.QueryOptions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.balancing.Server;
import ru.hh.jclient.consul.model.config.UpstreamServiceConsulConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UpstreamServiceImpl implements UpstreamService {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamServiceImpl.class);

  private final ServiceWeights defaultWeight = ImmutableServiceWeights.builder().passing(100).warning(10).build();
  private final HealthClient healthClient;

  private final Set<String> upstreamList;
  private final List<String> datacenterList;
  private final Map<String, String> lowercasedDataCenters;
  private final String currentDC;
  private final String currentNode;
  private final String currentServiceName;
  private final ConsistencyMode consistencyMode;
  private final int watchSeconds;
  private final boolean allowCrossDC;
  private final boolean healthPassing;
  private final boolean selfNodeFiltering;
  private Consumer<String> callback;

  private final ConcurrentMap<String, CopyOnWriteArrayList<Server>> serverList = new ConcurrentHashMap<>();

  public UpstreamServiceImpl(List<String> upstreamList, String currentServiceName, Consul consulClient, UpstreamServiceConsulConfig consulConfig) {
    if (upstreamList.isEmpty()) {
      throw new IllegalArgumentException("UpstreamList can't be empty");
    }
    if (consulConfig.getDatacenterList().isEmpty()) {
      throw new IllegalArgumentException("DatacenterList can't be empty");
    }

    this.healthClient = consulClient.healthClient();
    this.datacenterList = consulConfig.getDatacenterList();
    this.lowercasedDataCenters = datacenterList.stream().collect(toMap(String::toLowerCase, Function.identity()));
    this.upstreamList = Set.copyOf(upstreamList);
    this.currentDC = consulConfig.getCurrentDC();
    this.currentNode = consulConfig.getCurrentNode();
    this.currentServiceName = currentServiceName;
    this.allowCrossDC = consulConfig.isAllowCrossDC();
    this.healthPassing = consulConfig.isHealthPassing();
    this.selfNodeFiltering = consulConfig.isSelfNodeFilteringEnabled();
    this.watchSeconds = consulConfig.getWatchSeconds();
    this.consistencyMode = consulConfig.getConsistencyMode();
    if (!this.datacenterList.contains(this.currentDC)) {
      LOGGER.warn("datacenterList: {} doesn't consist currentDC {}", datacenterList, currentDC);
    }
    if (consulConfig.isSyncInit()) {
      LOGGER.debug("Trying to sync update servers");
      syncUpdateUpstreams();
    }
  }

  private void syncUpdateUpstreams() {
    for (String serviceName : upstreamList) {
      if (allowCrossDC) {
        ExecutorService executorService = Executors.newFixedThreadPool(datacenterList.size());
        var tasks = datacenterList.stream()
          .map(dc -> CompletableFuture.runAsync(() -> syncUpdateServiceInDC(serviceName, dc), executorService))
          .toArray(CompletableFuture[]::new);
        try {
          CompletableFuture.allOf(tasks).get();
          executorService.shutdown();
        } catch (InterruptedException | ExecutionException e) {
          if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }
          throw new RuntimeException(e);
        }
      } else {
        syncUpdateServiceInDC(serviceName, currentDC);
      }
    }
    var emptyUpstreams = findEmptyUpstreams();
    if (!emptyUpstreams.isEmpty()) {
      throw new IllegalStateException("There's no instances for services: " + emptyUpstreams);
    }
  }

  private Collection<String> findEmptyUpstreams() {
    var emptyUpstreams = new HashSet<>(upstreamList);
    var notEmptyServers = serverList.entrySet().stream()
      .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
      .map(Map.Entry::getKey)
      .collect(Collectors.toSet());
    emptyUpstreams.removeAll(notEmptyServers);
    return emptyUpstreams;
  }

  private void syncUpdateServiceInDC(String serviceName, String dataCenter) {
    QueryOptions queryOptions = ImmutableQueryOptions.builder()
      .datacenter(dataCenter.toLowerCase())
        .caller(currentServiceName)
        .consistencyMode(consistencyMode)
      .build();
    Map<ServiceHealthKey, ServiceHealth> state = healthClient.getHealthyServiceInstances(serviceName, queryOptions)
      .getResponse().stream()
      .collect(toMap(ServiceHealthKey::fromServiceHealth, Function.identity()));
    LOGGER.trace("Got {} for service={} in DC={}. Updating", state, serviceName, dataCenter);
    updateUpstreams(state, serviceName, dataCenter);
  }

  @Override
  public void setupListener(Consumer<String> callback) {
    this.callback = callback;
    upstreamList.forEach(this::subscribeToUpstream);
  }

  void notifyListeners() {
    upstreamList.forEach(callback);
  }

  private void subscribeToUpstream(String upstreamName) {
    if (allowCrossDC) {
      for (String dataCenter : datacenterList) {
        initializeCache(upstreamName, dataCenter);
      }
    } else {
      initializeCache(upstreamName, currentDC);
    }
  }

  private void initializeCache(String upstreamName, String datacenter) {
    QueryOptions queryOptions = ImmutableQueryOptions.builder()
        .datacenter(datacenter.toLowerCase())
        .caller(currentServiceName)
        .consistencyMode(consistencyMode)
        .build();
    ServiceHealthCache svHealth = ServiceHealthCache.newCache(healthClient, upstreamName, healthPassing, watchSeconds, queryOptions);

    svHealth.addListener((Map<ServiceHealthKey, ServiceHealth> newValues) -> {
      updateUpstreams(newValues, upstreamName, datacenter);
      notifyListeners();
      var emptyUpstreams = findEmptyUpstreams();
      if (!emptyUpstreams.isEmpty()) {
        LOGGER.debug("There's no instances for services: {}", emptyUpstreams);
      }
    });
    svHealth.start();
    LOGGER.info("subscribed to service {}; dc {}", upstreamName, datacenter);
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
    CopyOnWriteArrayList<Server> currentServers = serverList.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>());

    // assumption: no similar addresses in different DCs
    Map<String, Server> serverByAddress = currentServers.stream().collect(toMap(Server::getAddress, Function.identity()));
    for (ServiceHealth serviceHealth : upstreams.values()) {
      String nodeName = serviceHealth.getNode().getNode();
      if (selfNodeFiltering && notSameNode(nodeName)) {
        LOGGER.trace("Self node filtering activated. Skip: {}", nodeName);
        continue;
      }

      Service service = serviceHealth.getService();

      String address = Server.addressFromHostPort(getAddress(serviceHealth), service.getPort());
      String nodeDatacenter = serviceHealth.getNode().getDatacenter().map(this::restoreOriginalDataCenterName).orElse(null);
      int serverWeight = service.getWeights().orElse(defaultWeight).getPassing();

      Server server = serverByAddress.remove(address);

      if (server == null) {
        server = new Server(address, serverWeight, nodeDatacenter);
        currentServers.add(server);
      }

      server.setMeta(service.getMeta());
      server.setTags(service.getTags());
      server.setWeight(serverWeight);
    }
    var deadServersFromCurrentDC = serverByAddress.values().stream()
      .filter(server -> datacenter.equals(server.getDatacenter()))
      .collect(toList());
    LOGGER.debug("removing dead servers for {} in DC {}: {} ", serviceName, datacenter, deadServersFromCurrentDC);
    currentServers.removeAll(deadServersFromCurrentDC);

    LOGGER.info("upstreams for {} were updated in DC {}; servers: {} ", serviceName, datacenter,
        LOGGER.isDebugEnabled() ? currentServers : currentServers.size());
  }

  private boolean notSameNode(String nodeName) {
    return !StringUtils.isBlank(currentNode) && !currentNode.equalsIgnoreCase(nodeName);
  }

  private String restoreOriginalDataCenterName(String lowerCasedDcName) {
    String restoredDc = lowercasedDataCenters.get(lowerCasedDcName);
    if (restoredDc == null) {
      LOGGER.warn("Unable to restore original datacenter name for: {}", lowerCasedDcName);
      return lowerCasedDcName;
    }
    return restoredDc;
  }

  private static String getAddress(ServiceHealth serviceHealth) {
    String address = serviceHealth.getService().getAddress();
    if (!StringUtils.isBlank(address)) {
      return address;
    }

    return serviceHealth.getNode().getAddress();
  }
}
