package ru.hh.jclient.consul;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import ru.hh.consul.Consul;
import ru.hh.consul.HealthClient;
import ru.hh.consul.cache.ImmutableServiceHealthKey;
import ru.hh.consul.cache.ServiceHealthKey;
import ru.hh.consul.config.ClientConfig;
import ru.hh.consul.model.ConsulResponse;
import ru.hh.consul.model.catalog.ImmutableServiceWeights;
import ru.hh.consul.model.catalog.ServiceWeights;
import ru.hh.consul.model.health.HealthCheck;
import ru.hh.consul.model.health.ImmutableHealthCheck;
import ru.hh.consul.model.health.ImmutableNode;
import ru.hh.consul.model.health.ImmutableService;
import ru.hh.consul.model.health.ImmutableServiceHealth;
import ru.hh.consul.model.health.Node;
import ru.hh.consul.model.health.Service;
import ru.hh.consul.model.health.ServiceHealth;
import ru.hh.consul.monitoring.ClientEventCallback;
import ru.hh.consul.monitoring.ClientEventHandler;
import ru.hh.consul.option.ConsistencyMode;
import ru.hh.consul.option.QueryOptions;
import ru.hh.jclient.common.balancing.JClientInfrastructureConfig;
import ru.hh.jclient.common.balancing.Server;
import ru.hh.jclient.common.balancing.ServerStore;
import ru.hh.jclient.common.balancing.ServerStoreImpl;
import ru.hh.jclient.common.balancing.UpstreamManager;

public class UpstreamServiceImplTest {
  UpstreamServiceImpl upstreamService;
  static String SERVICE_NAME = "upstream1";
  static String NODE_NAME = "node123";
  static String DATA_CENTER = "DC1";
  static List<String> upstreamList = List.of(SERVICE_NAME);
  static List<String> datacenterList = List.of(DATA_CENTER);
  private HealthClient healthClient = mock(HealthClient.class);
  private UpstreamManager upstreamManager = mock(UpstreamManager.class);
  static Consul consulClient = mock(Consul.class);
  static int watchSeconds = 7;
  static boolean allowCrossDC = false;

  private ServerStore serverStore = new ServerStoreImpl();
  private static final JClientInfrastructureConfig infrastructureConfig = new JClientInfrastructureConfig() {

    @Override
    public String getServiceName() {
      return SERVICE_NAME;
    }

    @Override
    public String getCurrentDC() {
      return DATA_CENTER;
    }

    @Override
    public String getCurrentNodeName() {
      return NODE_NAME;
    }
  };
  UpstreamServiceConsulConfig consulConfig = new UpstreamServiceConsulConfig()
      .setUpstreams(upstreamList)
      .setHealthPassing(true)
      .setSelfNodeFilteringEnabled(true)
      .setDatacenterList(datacenterList)
      .setWatchSeconds(watchSeconds)
      .setSyncInit(false)
      .setConsistencyMode(ConsistencyMode.DEFAULT);

  @BeforeEach
  public void init() {
    when(healthClient.getConfig()).thenReturn(new ClientConfig());
    when(healthClient.getEventHandler()).thenReturn(new ClientEventHandler("", mock(ClientEventCallback.class)));
    when(consulClient.healthClient()).thenReturn(healthClient);
    mockServiceHealth(List.of());
    upstreamService = new UpstreamServiceImpl(infrastructureConfig, consulClient, serverStore, upstreamManager, consulConfig, List.of());
  }

  private void mockServiceHealth(List<ServiceHealth> health) {
    when(healthClient.getHealthyServiceInstances(anyString(), any(QueryOptions.class)))
        .thenReturn(new ConsulResponse<>(health, 0, false, BigInteger.ONE, Optional.empty()));
  }

  @Test
  public void testParseServer() {

    String address1 = "a1";
    String address2 = "a2";
    int weight = 12;
    int port1 = 124;
    int port2 = 126;

    ServiceHealth serviceHealth = buildServiceHealth(address1, port1, DATA_CENTER, NODE_NAME, weight, true);
    ServiceHealth serviceHealth2 = buildServiceHealth(address2, port2, DATA_CENTER, NODE_NAME, weight, true);

    Map<ServiceHealthKey, ServiceHealth> upstreams = new HashMap<>();
    upstreams.put(buildKey(address1), serviceHealth);
    upstreams.put(buildKey(address2), serviceHealth2);

    upstreamService.updateUpstreams(upstreams, SERVICE_NAME, DATA_CENTER);

    List<Server> servers = serverStore.getServers(SERVICE_NAME);
    assertEquals(2, servers.size());

    Map<String, Server> serverMap = servers.stream().collect(Collectors.toMap(Server::getAddress, Function.identity()));
    String address1FromHostPort = Server.addressFromHostPort(address1, port1);

    assertTrue(serverMap.containsKey(address1FromHostPort));
    Server server1 = serverMap.get(address1FromHostPort);
    assertNotNull(server1);
    assertEquals(weight, server1.getWeight());
    assertEquals(DATA_CENTER, server1.getDatacenter());

    assertTrue(serverMap.containsKey(Server.addressFromHostPort(address2, port2)));
  }

  @Test
  public void testUpdateReplacedServers() {

    String address1 = "a1";
    String address2 = "a2";
    String address3 = "a3";
    int weight = 12;
    int port1 = 124;
    int port2 = 126;
    int port3 = 127;

    ServiceHealth serviceHealth = buildServiceHealth(address1, port1, DATA_CENTER, NODE_NAME, weight, true);
    ServiceHealth serviceHealth2 = buildServiceHealth(address2, port2, DATA_CENTER, NODE_NAME, weight, true);
    ServiceHealth serviceHealth3 = buildServiceHealth(address3, port3, DATA_CENTER, NODE_NAME, weight, true);

    Map<ServiceHealthKey, ServiceHealth> upstreams = new HashMap<>();
    upstreams.put(buildKey(address1), serviceHealth);
    upstreams.put(buildKey(address2), serviceHealth2);

    upstreamService.updateUpstreams(upstreams, SERVICE_NAME, DATA_CENTER);

    List<Server> servers = serverStore.getServers(SERVICE_NAME);
    assertEquals(2, servers.size());

    upstreamService.updateUpstreams(Map.of(buildKey(address3), serviceHealth3), SERVICE_NAME, DATA_CENTER);
    List<Server> updatedServers = serverStore.getServers(SERVICE_NAME);

    assertEquals(1, updatedServers.size());
    Server server = updatedServers.get(0);
    assertEquals(Server.addressFromHostPort(address3, port3), server.getAddress());
    assertEquals(weight, server.getWeight());
    assertEquals(DATA_CENTER, server.getDatacenter());

    Server server2 = servers.get(1);
    assertEquals(Server.addressFromHostPort(address2, port2), server2.getAddress());
  }

  @Test
  public void testSameNode() {
    String address1 = "a1";
    int weight = 12;
    int port1 = 124;
    UpstreamServiceConsulConfig consulConfig = new UpstreamServiceConsulConfig()
        .setUpstreams(upstreamList)
        .setAllowCrossDC(allowCrossDC)
        .setHealthPassing(false)
        .setSelfNodeFilteringEnabled(false)
        .setDatacenterList(datacenterList)
        .setWatchSeconds(watchSeconds)
        .setConsistencyMode(ConsistencyMode.DEFAULT);

    ServiceHealth serviceHealth = buildServiceHealth(address1, port1, DATA_CENTER, NODE_NAME, weight, true);
    mockServiceHealth(List.of(serviceHealth));
    List<Server> servers = new UpstreamServiceImpl(infrastructureConfig, consulClient, serverStore, upstreamManager, consulConfig, List.of())
        .getUpstreamStore()
        .getServers(SERVICE_NAME);
    assertEquals(1, servers.size());
  }

  @Test
  public void testDifferentNodesInTest() {

    String address1 = "a1";
    String address2 = "a2";
    int weight = 12;
    int port1 = 124;
    int port2 = 126;
    UpstreamServiceConsulConfig consulConfig = new UpstreamServiceConsulConfig()
        .setUpstreams(upstreamList)
        .setAllowCrossDC(allowCrossDC)
        .setHealthPassing(false)
        .setSelfNodeFilteringEnabled(true)
        .setDatacenterList(datacenterList)
        .setWatchSeconds(watchSeconds)
        .setConsistencyMode(ConsistencyMode.DEFAULT);

    ServiceHealth serviceHealth = buildServiceHealth(address1, port1, DATA_CENTER, NODE_NAME, weight, true);
    ServiceHealth serviceHealth2 = buildServiceHealth(address2, port2, DATA_CENTER, "differentNode", weight, true);
    mockServiceHealth(List.of(serviceHealth, serviceHealth2));

    List<Server> servers = new UpstreamServiceImpl(infrastructureConfig, consulClient, serverStore, upstreamManager, consulConfig, List.of())
        .getUpstreamStore()
        .getServers(SERVICE_NAME);
    assertEquals(1, servers.size());
  }

  @Test
  public void testConcurrentUpdateServers() throws ExecutionException, InterruptedException {
    List<String> addresses = List.of("a1", "a2", "a3");
    List<String> dcS = IntStream.range(0, 1000).boxed().map(String::valueOf).collect(Collectors.toList());

    int weight = 12;
    int port = 124;

    var updatedUpstream = dcS
        .stream()
        .map(
            dc -> addresses
                .stream()
                .map(s -> buildServiceHealth(s, port, dc, NODE_NAME, weight, true))
                .collect(Collectors.toMap(s -> buildKey(s.getService().getAddress()), Function.identity()))
        )
        .map(
            dc -> CompletableFuture.runAsync(
                () -> upstreamService.updateUpstreams(dc, SERVICE_NAME, dc.values().stream().findFirst().get().getNode().getDatacenter().get()),
                Executors.newFixedThreadPool(dcS.size())
            )
        )
        .toArray(CompletableFuture[]::new);

    CompletableFuture.allOf(updatedUpstream).get();

    List<Server> servers = serverStore.getServers(SERVICE_NAME);
    assertEquals(addresses.size() * dcS.size(), servers.size());
  }

  @Test
  public void testDifferentNodesInProd() {
    String address1 = "a1";
    String address2 = "a2";
    int weight = 12;
    int port1 = 124;
    int port2 = 126;
    UpstreamServiceConsulConfig consulConfig = new UpstreamServiceConsulConfig()
        .setUpstreams(upstreamList)
        .setAllowCrossDC(allowCrossDC)
        .setHealthPassing(false)
        .setSelfNodeFilteringEnabled(false)
        .setDatacenterList(datacenterList)
        .setWatchSeconds(watchSeconds)
        .setConsistencyMode(ConsistencyMode.DEFAULT);

    ServiceHealth serviceHealth = buildServiceHealth(address1, port1, DATA_CENTER, NODE_NAME, weight, true);
    ServiceHealth serviceHealth2 = buildServiceHealth(address2, port2, DATA_CENTER, "differentNode", weight, true);
    mockServiceHealth(List.of(serviceHealth, serviceHealth2));

    List<Server> servers = new UpstreamServiceImpl(infrastructureConfig, consulClient, serverStore, upstreamManager, consulConfig, List.of())
        .getUpstreamStore()
        .getServers(SERVICE_NAME);
    assertEquals(2, servers.size());
  }

  @Test
  public void testNoServers() {
    mockServiceHealth(List.of());

    UpstreamServiceConsulConfig consulConfig = new UpstreamServiceConsulConfig()
        .setUpstreams(upstreamList)
        .setAllowCrossDC(allowCrossDC)
        .setHealthPassing(false)
        .setSelfNodeFilteringEnabled(false)
        .setDatacenterList(datacenterList)
        .setWatchSeconds(watchSeconds)
        .setConsistencyMode(ConsistencyMode.DEFAULT)
        .setSyncInit(true)
        .setIgnoreNoServersInCurrentDC(true);
    assertThrows(
        IllegalStateException.class,
        () -> new UpstreamServiceImpl(infrastructureConfig, consulClient, serverStore, upstreamManager, consulConfig, List.of())
    );

    var configWithNoCheck = consulConfig
        .setIgnoreNoServersUpstreams(List.of(SERVICE_NAME));
    assertEquals(
        0,
        new UpstreamServiceImpl(infrastructureConfig, consulClient, serverStore, upstreamManager, configWithNoCheck, List.of())
            .getUpstreamStore()
            .getServers(SERVICE_NAME)
            .size()
    );

    var noCheckForAnotherUpstream = configWithNoCheck
        .setIgnoreNoServersUpstreams(List.of("some_another_upstream"));
    assertThrows(
        IllegalStateException.class,
        () -> new UpstreamServiceImpl(infrastructureConfig, consulClient, serverStore, upstreamManager, noCheckForAnotherUpstream, List.of())
    );
  }

  @Test
  public void testNoServersInCurrentDc() {
    ServiceHealth serviceHealth = buildServiceHealth("a1", 1, "notCurrentDc", NODE_NAME, 100, true);
    mockServiceHealth(List.of(serviceHealth));

    UpstreamServiceConsulConfig consulConfig = new UpstreamServiceConsulConfig()
        .setUpstreams(upstreamList)
        .setCrossDCUpstreams(upstreamList)
        .setAllowCrossDC(true)
        .setHealthPassing(false)
        .setSelfNodeFilteringEnabled(false)
        .setDatacenterList(List.of(DATA_CENTER, "notCurrentDc"))
        .setWatchSeconds(watchSeconds)
        .setConsistencyMode(ConsistencyMode.DEFAULT)
        .setSyncInit(true)
        .setIgnoreNoServersInCurrentDC(false);
    assertThrows(
        IllegalStateException.class,
        () -> new UpstreamServiceImpl(infrastructureConfig, consulClient, serverStore, upstreamManager, consulConfig, List.of())
    );

    UpstreamServiceConsulConfig ignoreConfig = UpstreamServiceConsulConfig.copyOf(consulConfig).setIgnoreNoServersInCurrentDC(true);
    assertEquals(
        1,
        new UpstreamServiceImpl(infrastructureConfig, consulClient, serverStore, upstreamManager, ignoreConfig, List.of())
            .getUpstreamStore()
            .getServers(SERVICE_NAME)
            .size()
    );

    serviceHealth = buildServiceHealth("a2", 1, DATA_CENTER.toLowerCase(), NODE_NAME, 100, true);
    mockServiceHealth(List.of(serviceHealth));
    assertEquals(
        1,
        new UpstreamServiceImpl(infrastructureConfig, consulClient, serverStore, upstreamManager, consulConfig, List.of())
            .getUpstreamStore()
            .getServers(SERVICE_NAME)
            .size()
    );
  }

  private ServiceHealth buildServiceHealth(String address, int port, String datacenter, String nodeName, int weight, boolean passing) {
    Service service = buildService(address, port, buildWeight(weight));
    HealthCheck healthCheck = buildHealthCheck(passing);
    return ImmutableServiceHealth
        .builder()
        .node(buildNode(datacenter, nodeName))
        .service(service)
        .addChecks(healthCheck)
        .build();
  }

  private ServiceHealthKey buildKey(String address) {
    return ImmutableServiceHealthKey.builder().serviceId("serviceid").host(address).port(156).build();
  }

  private ServiceWeights buildWeight(int weight) {
    return ImmutableServiceWeights.builder().passing(weight).warning(2).build();
  }

  private Service buildService(String address, int port, ServiceWeights serviceWeights) {
    return ImmutableService
        .builder()
        .address(address)
        .id("id1")
        .service(SERVICE_NAME)
        .port(port)
        .weights(serviceWeights)
        .build();
  }

  private HealthCheck buildHealthCheck(boolean passing) {
    return ImmutableHealthCheck
        .builder()
        .name(SERVICE_NAME)
        .node("node1")
        .checkId("check1")
        .status(passing ? "passing" : "failed")
        .build();
  }

  private Node buildNode(String datacenter, String nodeName) {
    return ImmutableNode.builder().node(nodeName).address("address1").datacenter(datacenter).build();
  }
}
