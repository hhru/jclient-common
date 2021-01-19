package ru.hh.jclient.consul;

import com.orbitz.consul.Consul;
import com.orbitz.consul.cache.ImmutableServiceHealthKey;
import com.orbitz.consul.cache.ServiceHealthKey;
import com.orbitz.consul.model.catalog.ImmutableServiceWeights;
import com.orbitz.consul.model.catalog.ServiceWeights;
import com.orbitz.consul.model.health.HealthCheck;
import com.orbitz.consul.model.health.ImmutableHealthCheck;
import com.orbitz.consul.model.health.ImmutableNode;
import com.orbitz.consul.model.health.ImmutableService;
import com.orbitz.consul.model.health.ImmutableServiceHealth;
import com.orbitz.consul.model.health.Node;
import com.orbitz.consul.model.health.Service;
import com.orbitz.consul.model.health.ServiceHealth;
import com.orbitz.consul.option.ConsistencyMode;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import ru.hh.jclient.common.balancing.Server;
import ru.hh.jclient.consul.model.config.UpstreamServiceConsulConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpstreamServiceImplTest {
  static UpstreamServiceImpl upstreamService;
  static String SERVICE_NAME = "upstream1";
  static String NODE_NAME = "node123";
  static String DATA_CENTER = "DC1";
  static List<String> upstreamList = List.of(SERVICE_NAME);
  static List<String> datacenterList = List.of(DATA_CENTER);
  static Consul consulClient = mock(Consul.class);
  static int watchSeconds = 7;
  static boolean allowCrossDC = false;

  @Before
  public void init() {
    UpstreamServiceConsulConfig consulConfig = new UpstreamServiceConsulConfig()
        .setAllowCrossDC(allowCrossDC)
        .setHealthPassing(true)
        .setSelfNodeFilteringEnabled(true)
        .setDatacenterList(datacenterList)
        .setWatchSeconds(watchSeconds)
        .setCurrentDC(DATA_CENTER)
        .setCurrentNode(null)
        .setConsistencyMode(ConsistencyMode.DEFAULT);
    upstreamService = new UpstreamServiceImpl(upstreamList, consulClient, consulConfig);
  }

  @Test
  public void testNotify() {
    List<String> consumerMock = new ArrayList<>();

    try {
      upstreamService.setupListener(consumerMock::add);
    } catch (Exception ex) {
      //ignore
    }
    upstreamService.notifyListeners();

    assertEquals(1, consumerMock.size());

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

    List<Server> servers = upstreamService.getServers(SERVICE_NAME);
    servers.sort(Comparator.comparing(Server::getAddress));
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
        .setAllowCrossDC(allowCrossDC)
        .setHealthPassing(false)
        .setSelfNodeFilteringEnabled(false)
        .setDatacenterList(datacenterList)
        .setWatchSeconds(watchSeconds)
        .setCurrentDC(DATA_CENTER)
        .setCurrentNode(NODE_NAME)
        .setConsistencyMode(ConsistencyMode.DEFAULT);

    UpstreamServiceImpl upstreamService = new UpstreamServiceImpl(upstreamList, consulClient, consulConfig);

    ServiceHealth serviceHealth = buildServiceHealth(address1, port1, DATA_CENTER, NODE_NAME, weight, true);

    Map<ServiceHealthKey, ServiceHealth> upstreams = new HashMap<>();
    upstreams.put(buildKey(address1), serviceHealth);

    upstreamService.updateUpstreams(upstreams, SERVICE_NAME, DATA_CENTER);

    List<Server> servers = upstreamService.getServers(SERVICE_NAME);
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
        .setAllowCrossDC(allowCrossDC)
        .setHealthPassing(false)
        .setSelfNodeFilteringEnabled(true)
        .setDatacenterList(datacenterList)
        .setWatchSeconds(watchSeconds)
        .setCurrentDC(DATA_CENTER)
        .setCurrentNode(NODE_NAME)
        .setConsistencyMode(ConsistencyMode.DEFAULT);

    UpstreamServiceImpl upstreamService = new UpstreamServiceImpl(upstreamList, consulClient, consulConfig);

    ServiceHealth serviceHealth = buildServiceHealth(address1, port1, DATA_CENTER, NODE_NAME, weight, true);
    ServiceHealth serviceHealth2 = buildServiceHealth(address2, port2, DATA_CENTER, "differentNode", weight, true);

    Map<ServiceHealthKey, ServiceHealth> upstreams = new HashMap<>();
    upstreams.put(buildKey(address1), serviceHealth);
    upstreams.put(buildKey(address2), serviceHealth2);

    upstreamService.updateUpstreams(upstreams, SERVICE_NAME, DATA_CENTER);

    List<Server> servers = upstreamService.getServers(SERVICE_NAME);
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
        .setAllowCrossDC(allowCrossDC)
        .setHealthPassing(false)
        .setSelfNodeFilteringEnabled(false)
        .setDatacenterList(datacenterList)
        .setWatchSeconds(watchSeconds)
        .setCurrentDC(DATA_CENTER)
        .setCurrentNode(NODE_NAME)
        .setConsistencyMode(ConsistencyMode.DEFAULT);

    UpstreamServiceImpl upstreamService = new UpstreamServiceImpl(upstreamList, consulClient, consulConfig);

    ServiceHealth serviceHealth = buildServiceHealth(address1, port1, DATA_CENTER, NODE_NAME, weight, true);
    ServiceHealth serviceHealth2 = buildServiceHealth(address2, port2, DATA_CENTER, "differentNode", weight, true);

    Map<ServiceHealthKey, ServiceHealth> upstreams = new HashMap<>();
    upstreams.put(buildKey(address1), serviceHealth);
    upstreams.put(buildKey(address2), serviceHealth2);

    upstreamService.updateUpstreams(upstreams, SERVICE_NAME, DATA_CENTER);

    List<Server> servers = upstreamService.getServers(SERVICE_NAME);
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
