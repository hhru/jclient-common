package ru.hh.jclient.common.balancing;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT;
import static ru.hh.jclient.common.balancing.UpstreamConfig.getDefaultConfig;
import static ru.hh.jclient.common.balancing.UpstreamConfigParserTest.buildTestConfig;
import ru.hh.jclient.consul.model.ApplicationConfig;

import java.util.List;

public class UpstreamTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamTest.class);
  private static final String TEST_SERVICE_NAME = "backend";

  private static final String TEST_HOST_CUSTOM_PROFILE = "foo";
  UpstreamConfig config = UpstreamConfig.fromApplicationConfig(new ApplicationConfig(), DEFAULT, DEFAULT);

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
    assertEquals("a", upstream.acquireServer().getAddress());
    assertServerCounters(servers, 0, 1, 1, 0);
    assertServerCounters(servers, 1, 0, 0, 0);

    assertEquals("b", upstream.acquireServer().getAddress());
    assertServerCounters(servers, 1, 1, 1, 0);

    assertEquals("b", upstream.acquireServer().getAddress());
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
  public void acquireReleaseWhenMaxFailsIsZero() {
    List<Server> servers = List.of(new Server("a", 1, null));

    ApplicationConfig applicationConfig = buildTestConfig();

    UpstreamConfig config = UpstreamConfig.fromApplicationConfig(applicationConfig, DEFAULT, DEFAULT);

    Upstream upstream = createTestUpstream(TEST_SERVICE_NAME, servers, config);
    int index = upstream.acquireServer().getIndex();
    upstream.releaseServer(index, true, 100);

    int serverIndex = 0;

    assertEquals(serverIndex, upstream.acquireServer().getIndex());
  }

  @Test
  public void acquireReleaseFromTwoThreads() throws Exception {
    int numOfRequests = 100_000;
    int tests = 100;
    int weight = numOfRequests * tests * 2 + 1;
    List<Server> servers = List.of(new Server("a", weight, null));

    Upstream upstream = new Upstream(TEST_SERVICE_NAME, config, servers);
    Server server = servers.get(0);

    Runnable acquireReleaseTask = () -> acquireReleaseUpstream(upstream, numOfRequests);

    for (int t = 1; t <= tests; t++) {
      long start = currentTimeMillis();
      Thread thread = new Thread(acquireReleaseTask);
      thread.start();

      acquireReleaseUpstream(upstream, numOfRequests);

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

  private static void acquireReleaseUpstream(Upstream upstream, int times) {
    for (int i = 0; i < times; i++) {
      ServerEntry serverEntry = upstream.acquireServer();
      if (serverEntry != null) {
        upstream.releaseServer(serverEntry.getIndex(), false, 100);
      }
    }
  }

  private static Upstream createTestUpstream(String serviceName, List<Server> servers) {
    return createTestUpstream(serviceName, servers, getDefaultConfig());
  }

  private static Upstream createTestUpstream(String serviceName, List<Server> servers, UpstreamConfig config) {
    return new Upstream(serviceName, config, servers);
  }

  private static List<Server> buildServers(){
    return List.of(new Server("a", 1, null), new Server("b", 2, null));
  }
}
