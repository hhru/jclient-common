package ru.hh.jclient.common.balancing;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT;
import ru.hh.jclient.consul.ValueNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class UpstreamTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamTest.class);
  private static final String TEST_SERVICE_NAME = "backend";

  private static final String TEST_HOST_CUSTOM_PROFILE = "foo";
  UpstreamConfig config = UpstreamConfig.fromTree(DEFAULT, DEFAULT, DEFAULT, new ValueNode());

  @Test
  public void createUpstreamServiceOnly() {
    Upstream upstream = createTestUpstream(TEST_SERVICE_NAME, List.of());
    assertEquals(TEST_SERVICE_NAME, upstream.getName());
  }

  @Test
  public void createUpstreamFull() {
    Upstream.UpstreamKey upstreamKey = new Upstream.UpstreamKey(TEST_SERVICE_NAME, TEST_HOST_CUSTOM_PROFILE);
    Upstream upstream = createTestUpstream(String.join(":", TEST_SERVICE_NAME, TEST_HOST_CUSTOM_PROFILE), List.of());

    assertEquals(upstreamKey, upstream.getKey());
  }

  @Test
  public void acquireServer() {
    List<Server> servers = buildServers();
    Upstream upstream = createTestUpstream(TEST_SERVICE_NAME, servers);
    assertEquals("a", upstream.acquireServer(servers).getAddress());
    assertServerCounters(servers, 0, 1, 1, 0);
    assertServerCounters(servers, 1, 0, 0, 0);

    assertEquals("b", upstream.acquireServer(servers).getAddress());
    assertServerCounters(servers, 1, 1, 1, 0);

    assertEquals("b", upstream.acquireServer(servers).getAddress());
    assertServerCounters(servers, 1, 2, 2, 0);

    upstream.releaseServer(0, false, 100);
    upstream.releaseServer(1, false, 100);

    assertServerCounters(servers, 0, 0, 0, 0);
    assertServerCounters(servers, 1, 1, 0, 0);

    upstream.releaseServer(1, false, 100);

    assertServerCounters(servers, 0, 0, 0, 0);
    assertServerCounters(servers, 1, 0, 0, 0);
  }

  @Test
  public void acquireInactiveServer() {
    List<Server> servers = buildServers();
    Upstream upstream = createTestUpstream(TEST_SERVICE_NAME, servers);
    servers.forEach(server -> server.deactivate(1, mock(ScheduledExecutorService.class)));

    assertNull(upstream.acquireServer(servers));

    assertServerCounters(servers, 0, 0, 0, 0);
    assertServerCounters(servers, 1, 0, 0, 0);

    servers.get(1).activate();

    assertEquals("b", upstream.acquireServer(servers).getAddress());

    assertServerCounters(servers, 1, 1, 1, 0);
  }

  @Test
  public void acquireExcludedServer() {
    List<Server> servers = buildServers();
    Upstream upstream = createTestUpstream(TEST_SERVICE_NAME, servers);

    int excludedServerIndex = 0;

    ServerEntry serverEntry = upstream.acquireServer(singleton(excludedServerIndex));

    assertEquals("b", serverEntry.getAddress());

    assertEquals(0, servers.get(0).getRequests());
    assertEquals(1, servers.get(1).getRequests());
  }

  @Test
  public void acquireReleaseServerWithFails() {
    Map<String, ValueNode> values = new HashMap<>();
    List<Server> servers = List.of(new Server("a", 1, null));
    Upstream upstream = createTestUpstream(TEST_SERVICE_NAME, servers, buildValueNode(values));
    int serverIndex = 0;
    Server server = servers.get(serverIndex);
    assertEquals(0, server.getFails());

    assertEquals(serverIndex, upstream.acquireServer(servers).getIndex());
    upstream.releaseServer(serverIndex, true, 100);

    assertFalse(server.isActive());

    assertEquals(1, server.getFails());

    assertNull(upstream.acquireServer(servers));
  }

  @Test
  public void acquireReleaseWhenMaxFailsIsZero() {
    List<Server> servers = List.of(new Server("a", 1, null));
    Map<String, ValueNode> values = new HashMap<>();
    values.put("max_fails", new ValueNode("0"));
    values.put("fail_timeout_sec", new ValueNode("0.1"));

    Upstream upstream = createTestUpstream(TEST_SERVICE_NAME, servers, buildValueNode(values));
    int index = upstream.acquireServer(servers).getIndex();
    upstream.releaseServer(index, true, 100);

    int serverIndex = 0;

    assertEquals(serverIndex, upstream.acquireServer(servers).getIndex());
  }

  @Test
  public void acquireReleaseFromTwoThreads() throws Exception {
    int numOfRequests = 100_000;
    int tests = 100;
    int weight = numOfRequests * tests * 2 + 1;
    List<Server> servers = List.of(new Server("a", weight, null));

    Upstream upstream = new Upstream(TEST_SERVICE_NAME, config, servers, mock(ScheduledExecutorService.class));
    Server server = servers.get(0);

    Runnable acquireReleaseTask = () -> acquireReleaseUpstream(upstream, numOfRequests, servers);

    for (int t = 1; t <= tests; t++) {
      long start = currentTimeMillis();
      Thread thread = new Thread(acquireReleaseTask);
      thread.start();

      acquireReleaseUpstream(upstream, numOfRequests, servers);

      thread.join();

      assertEquals("current requests", 0, server.getRequests());
      assertEquals("current fails", 0, server.getFails());

      LOGGER.info("finished iteration {} out of {} in {} ms", t, tests, (currentTimeMillis() - start));
    }

    assertEquals("stats requests", weight - 1, server.getStatsRequests());
  }

  private static void assertServerCounters(List<Server> servers, int serverIndex, int requests, int statsRequests, int fails) {

    assertEquals("requests", requests, servers.get(serverIndex).getRequests());
    assertEquals("statsRequests", statsRequests, servers.get(serverIndex).getStatsRequests());
    assertEquals("fails", fails, servers.get(serverIndex).getFails());
  }

  private static void acquireReleaseUpstream(Upstream upstream, int times, List<Server> servers) {
    for (int i = 0; i < times; i++) {
      ServerEntry serverEntry = upstream.acquireServer(servers);
      if (serverEntry != null) {
        upstream.releaseServer(serverEntry.getIndex(), false, 100);
      }
    }
  }

  private static Upstream createTestUpstream(String serviceName, List<Server> servers) {
    return createTestUpstream(serviceName, servers, new ValueNode());
  }

  private static Upstream createTestUpstream(String serviceName, List<Server> servers, ValueNode valueNode) {
    UpstreamConfig config = UpstreamConfig.fromTree(serviceName, DEFAULT, DEFAULT, valueNode);
    return new Upstream(serviceName, config, servers, mock(ScheduledExecutorService.class));
  }

  private static List<Server> buildServers(){
    return List.of(new Server("a", 1, null), new Server("b", 2, null));
  }

  private ValueNode buildValueNode(Map<String, ValueNode> values) {
    ValueNode rootNode = new ValueNode();
    ValueNode serviceNode = rootNode.computeMapIfAbsent(TEST_SERVICE_NAME);
    ValueNode hostNode = serviceNode.computeMapIfAbsent(DEFAULT);
    ValueNode profileNode = hostNode.computeMapIfAbsent(UpstreamConfig.PROFILE_NODE);
    ValueNode defaultProfile = profileNode.computeMapIfAbsent(DEFAULT);
    defaultProfile.putAll(values);
    return rootNode;
  }
}
