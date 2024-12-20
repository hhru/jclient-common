package ru.hh.jclient.consul;

import io.netty.util.NetUtil;
import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toMap;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.consul.Consul;
import ru.hh.consul.HealthClient;
import ru.hh.consul.cache.ConsulCache;
import ru.hh.consul.cache.ServiceHealthCache;
import ru.hh.consul.cache.ServiceHealthKey;
import ru.hh.consul.model.ConsulResponse;
import ru.hh.consul.model.catalog.ImmutableServiceWeights;
import ru.hh.consul.model.catalog.ServiceWeights;
import ru.hh.consul.model.health.Service;
import ru.hh.consul.model.health.ServiceHealth;
import ru.hh.consul.option.ConsistencyMode;
import ru.hh.consul.option.ImmutableQueryOptions;
import ru.hh.consul.option.QueryOptions;
import ru.hh.jclient.common.balancing.JClientInfrastructureConfig;
import static ru.hh.jclient.common.balancing.PropertyKeys.IGNORE_NO_SERVERS_IN_CURRENT_DC_KEY;
import static ru.hh.jclient.common.balancing.PropertyKeys.SYNC_UPDATE_KEY;
import ru.hh.jclient.common.balancing.Server;
import ru.hh.jclient.common.balancing.ServerStore;
import ru.hh.jclient.common.balancing.UpstreamManager;

public class UpstreamServiceImpl implements AutoCloseable, UpstreamService {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamServiceImpl.class);

  private final ServiceWeights defaultWeight = ImmutableServiceWeights.builder().passing(100).warning(10).build();
  private final HealthClient healthClient;
  private final ServerStore serverStore;
  private final Collection<Consumer<Collection<String>>> callbacks;

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
  private final boolean httpCompressionEnabled;

  private final Map<String, BigInteger> initialIndexes;
  private final List<ServiceHealthCache> serviceHealthCaches = new CopyOnWriteArrayList<>();


  public UpstreamServiceImpl(
      JClientInfrastructureConfig infrastructureConfig,
      Consul consulClient,
      ServerStore serverStore,
      UpstreamManager upstreamManager,
      UpstreamServiceConsulConfig consulConfig,
      Collection<Consumer<Collection<String>>> upstreamUpdateCallbacks
  ) {
    LOGGER.info("config: {}", consulConfig);
    this.upstreamList = Set.copyOf(consulConfig.getUpstreams());
    if (this.upstreamList.isEmpty()) {
      throw new IllegalArgumentException("UpstreamList can't be empty");
    }
    if (consulConfig.getDatacenterList() == null || consulConfig.getDatacenterList().isEmpty()) {
      throw new IllegalArgumentException("DatacenterList can't be empty");
    }
    this.serverStore = serverStore;
    this.callbacks = Stream
        .of(upstreamUpdateCallbacks.stream(), Stream.of((Consumer<Collection<String>>) upstreamManager::updateUpstreams))
        .flatMap(Function.identity())
        .collect(Collectors.toList());
    this.currentServiceName = infrastructureConfig.getServiceName();
    this.currentDC = infrastructureConfig.getCurrentDC();
    this.currentNode = infrastructureConfig.getCurrentNodeName();
    this.healthClient = consulClient.healthClient();
    this.datacenterList = consulConfig.getDatacenterList();
    this.lowercasedDataCenters = datacenterList.stream().collect(toMap(String::toLowerCase, Function.identity()));
    this.allowCrossDC = consulConfig.isAllowCrossDC();
    this.healthPassing = consulConfig.isHealthPassing();
    this.selfNodeFiltering = consulConfig.isSelfNodeFilteringEnabled();
    this.watchSeconds = consulConfig.getWatchSeconds();
    this.consistencyMode = consulConfig.getConsistencyMode();
    this.httpCompressionEnabled = consulConfig.isHttpCompressionEnabled();
    if (!this.datacenterList.contains(this.currentDC)) {
      LOGGER.warn("datacenterList: {} doesn't consist currentDC {}", datacenterList, currentDC);
    }

    if (consulConfig.isSyncInit()) {
      LOGGER.info("Sync update servers");
      this.initialIndexes = new ConcurrentHashMap<>(datacenterList.size());
      syncUpdateUpstreams();
      checkServersForAllUpstreamsExist(true, consulConfig.getIgnoreNoServersUpstreams());
      if (!consulConfig.isIgnoreNoServersInCurrentDC()) {
        checkServersForAllUpstreamsInCurrentDcExist(true);
      }
      this.callbacks.forEach(cb -> cb.accept(upstreamList));
    } else {
      this.initialIndexes = Map.of();
    }
    upstreamList.forEach(this::subscribeToUpstream);
  }

  private void syncUpdateUpstreams() {
    for (String serviceName : upstreamList) {
      if (allowCrossDC) {
        ExecutorService executorService = Executors.newFixedThreadPool(datacenterList.size());
        var tasks = datacenterList
            .stream()
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
  }

  private void syncUpdateServiceInDC(String serviceName, String dataCenter) {
    QueryOptions queryOptions = buildQueryOptions(dataCenter);

    ConsulResponse<List<ServiceHealth>> response = healthClient.getHealthyServiceInstances(serviceName, queryOptions);
    initialIndexes.put(dataCenter, response.getIndex());
    Map<ServiceHealthKey, ServiceHealth> state = response
        .getResponse()
        .stream()
        .collect(toMap(ServiceHealthKey::fromServiceHealth, Function.identity()));
    LOGGER.trace("Got {} for service={} in DC={}. Updating", state, serviceName, dataCenter);
    updateUpstreams(state, serviceName, dataCenter);
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

  private QueryOptions buildQueryOptions(String datacenter) {
    ImmutableQueryOptions.Builder queryOptions = ImmutableQueryOptions
        .builder()
        .datacenter(datacenter.toLowerCase())
        .caller(currentServiceName)
        .consistencyMode(consistencyMode);
    if (!httpCompressionEnabled) {
      queryOptions.header(Map.of("Accept-Encoding", "identity"));
    }
    return queryOptions.build();
  }

  private void initializeCache(String upstreamName, String datacenter) {
    QueryOptions queryOptions = buildQueryOptions(datacenter);
    ServiceHealthCache svHealth = ServiceHealthCache.newCache(
        healthClient,
        upstreamName,
        healthPassing,
        watchSeconds,
        initialIndexes.get(datacenter),
        queryOptions
    );

    svHealth.addListener((Map<ServiceHealthKey, ServiceHealth> newValues) -> {
      updateUpstreams(newValues, upstreamName, datacenter);
      checkServersForAllUpstreamsExist(false, List.of());
      checkServersForAllUpstreamsInCurrentDcExist(false);

      callbacks.forEach(cb -> cb.accept(Set.of(upstreamName)));
    });
    serviceHealthCaches.add(svHealth);
    svHealth.start();
    LOGGER.info("subscribed to service {}; dc {}", upstreamName, datacenter);
  }

  private void checkServersForAllUpstreamsInCurrentDcExist(boolean throwIfError) {
    var upstreamsNotPresentInCurrentDC = upstreamList
        .stream()
        .filter(upstream -> serverStore.getServers(upstream).stream().noneMatch(this::isInCurrentDc))
        .collect(Collectors.toSet());
    if (!upstreamsNotPresentInCurrentDC.isEmpty()) {
      if (throwIfError) {
        throw new IllegalStateException("There's no instances in DC " + currentDC + " for services: " + upstreamsNotPresentInCurrentDC
            + ". If it is intentional config use " + IGNORE_NO_SERVERS_IN_CURRENT_DC_KEY + " property to disable this check"
        );
      }
      LOGGER.warn("There's no instances in DC {} for services: {}", currentDC, upstreamsNotPresentInCurrentDC);
    }
  }

  private boolean isInCurrentDc(Server server) {
    return currentDC == null || currentDC.equals(server.getDatacenter());
  }

  private void checkServersForAllUpstreamsExist(boolean throwIfError, List<String> ignoreNoServersUpstreams) {
    var emptyUpstreams = upstreamList
        .stream()
        .filter(upstream -> !ignoreNoServersUpstreams.contains(upstream))
        .filter(upstream -> serverStore.getServers(upstream).isEmpty())
        .collect(Collectors.toSet());
    if (!emptyUpstreams.isEmpty()) {
      if (throwIfError) {
        throw new IllegalStateException("There's no instances for services: " + emptyUpstreams
            + ". If it is intentional config use " + SYNC_UPDATE_KEY + " property to disable this check");
      }
      LOGGER.warn("There's no instances for services: {}", emptyUpstreams);
    }
  }

  void updateUpstreams(Map<ServiceHealthKey, ServiceHealth> upstreams, String upstreamName, String datacenter) {
    // ServerStore is backed by a SET which MUST have ordering guarantees
    LinkedHashSet<Server> currentServers = serverStore
        .getServers(upstreamName)
        .stream()
        .filter(server -> datacenter.equals(server.getDatacenter()))
        .collect(Collectors.toCollection(LinkedHashSet::new));

    Map<String, Server> serverToRemoveByAddress = currentServers.stream().collect(toMap(Server::getAddress, Function.identity()));

    for (ServiceHealth serviceHealth : upstreams.values()) {
      String nodeName = serviceHealth.getNode().getNode();
      if (selfNodeFiltering && notSameNode(nodeName)) {
        LOGGER.trace("Self node filtering activated. Skip: {}", nodeName);
        continue;
      }

      Service service = serviceHealth.getService();

      // A known constraint. We do not allow upstream names because it floods DNS server with resolve requests.
      String ipAddress = serviceHealth.getService().getAddress();
      if (!isValidIpAddress(ipAddress)) {
        LOGGER.warn("Invalid ip address supplied {}", ipAddress);
        continue;
      }

      String address = Server.addressFromHostPort(ipAddress, service.getPort());
      String nodeDatacenter = serviceHealth.getNode().getDatacenter().map(this::restoreOriginalDataCenterName).orElse(null);
      int serverWeight = service.getWeights().orElse(defaultWeight).getPassing();

      Server server = serverToRemoveByAddress.remove(address);

      if (server == null) {
        server = new Server(address, nodeName, serverWeight, nodeDatacenter);
        currentServers.add(server);
      }
      server.update(serverWeight, service.getMeta(), service.getTags());
    }
    serverStore.updateServers(upstreamName, currentServers, serverToRemoveByAddress.values());
    LOGGER.info(
        "upstreams for {} were updated in DC {}; alive servers: {}, dead servers: {}",
        upstreamName,
        datacenter,
        LOGGER.isDebugEnabled() ? currentServers : currentServers.size(),
        LOGGER.isDebugEnabled() ? serverToRemoveByAddress.values() : serverToRemoveByAddress.values().size()
    );
  }

  private static boolean isValidIpAddress(String address) {
    return NetUtil.isValidIpV4Address(address) || NetUtil.isValidIpV6Address(address);
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

  ServerStore getUpstreamStore() {
    return serverStore;
  }

  @Override
  public void close() {
    serviceHealthCaches.forEach(ConsulCache::close);
  }
}
