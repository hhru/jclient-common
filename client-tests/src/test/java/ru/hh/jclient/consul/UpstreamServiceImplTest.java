package ru.hh.jclient.consul;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import ru.hh.consul.option.QueryOptions;
import ru.hh.jclient.common.balancing.Server;
import ru.hh.jclient.common.balancing.ServerStore;
import ru.hh.jclient.common.balancing.ServerStoreImpl;
import ru.hh.jclient.common.balancing.UpstreamManager;
import ru.hh.jclient.common.balancing.JClientInfrastructureConfig;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

  @Before
  public void init() {
    when(healthClient.getConfig()).thenReturn(new ClientConfig());
    when(healthClient.getEventHandler()).thenReturn(new ClientEventHandler("", mock(ClientEventCallback.class)));
    when(consulClient.healthClient()).thenReturn(healthClient);
    mockServiceHealth(List.of());
    upstreamService = new UpstreamServiceImpl(infrastructureConfig, consulClient, consulConfig, serverStore, upstreamManager, List.of());
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

    Server server = servers.get(0);
    assertEquals(Server.addressFromHostPort(address1, port1), server.getAddress());
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
    List<Server> servers = new UpstreamServiceImpl(infrastructureConfig, consulClient, consulConfig, serverStore, upstreamManager, List.of())
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

    List<Server> servers = new UpstreamServiceImpl(infrastructureConfig, consulClient, consulConfig, serverStore, upstreamManager, List.of())
      .getUpstreamStore()
      .getServers(SERVICE_NAME);
    assertEquals(1, servers.size());
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

    List<Server> servers = new UpstreamServiceImpl(infrastructureConfig, consulClient, consulConfig, serverStore, upstreamManager, List.of())
      .getUpstreamStore()
      .getServers(SERVICE_NAME);
    assertEquals(2, servers.size());
  }

  private ServiceHealth buildServiceHealth(String address, int port, String datacenter, String nodeName, int weight, boolean passing) {
    Service service = buildService(address, port, buildWeight(weight));
    HealthCheck healthCheck = buildHealthCheck(passing);
    return ImmutableServiceHealth.builder()
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
    return ImmutableService.builder().address(address)
            .id("id1")
            .service(SERVICE_NAME)
            .port(port)
            .weights(serviceWeights).build();
  }

  private HealthCheck buildHealthCheck(boolean passing) {
    return ImmutableHealthCheck.builder()
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
