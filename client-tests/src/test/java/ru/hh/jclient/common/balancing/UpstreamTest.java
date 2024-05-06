package ru.hh.jclient.common.balancing;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singleton;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT;
import static ru.hh.jclient.common.balancing.UpstreamConfigs.getDefaultConfig;
import ru.hh.jclient.common.balancing.config.ApplicationConfig;
import static ru.hh.jclient.common.balancing.config.ApplicationConfigTest.buildTestConfig;

public class UpstreamTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamTest.class);
  private static final String TEST_SERVICE_NAME = "backend";
  UpstreamConfigs configMap = ApplicationConfig.toUpstreamConfigs(new ApplicationConfig(), DEFAULT);

  @Test
  public void createUpstreamServiceOnly() {
    Upstream upstream = createTestUpstream(TEST_SERVICE_NAME, List.of());
    assertEquals(TEST_SERVICE_NAME, upstream.getName());
  }

  @Test
  public void acquireServer() {
    List<Server> servers = buildServers();
    Upstream upstream = createTestUpstream(TEST_SERVICE_NAME, servers);
    assertEquals("a", upstream.acquireServer().getAddress());
    assertServerCounters(servers, 0, 1, 1);
    assertServerCounters(servers, 1, 0, 0);

    assertEquals("b", upstream.acquireServer().getAddress());
    assertServerCounters(servers, 1, 1, 1);

    assertEquals("b", upstream.acquireServer().getAddress());
    assertServerCounters(servers, 1, 2, 2);

    upstream.releaseServer(0, false, false, 100);
    upstream.releaseServer(1, false, false, 100);

    assertServerCounters(servers, 0, 0, 1);
    assertServerCounters(servers, 1, 1, 2);

    upstream.releaseServer(1, false, false, 100);

    assertServerCounters(servers, 0, 0, 1);
    assertServerCounters(servers, 1, 0, 2);
  }

  @Test
  public void acquireExcludedServer() {
    List<Server> servers = buildServers();
    Upstream upstream = createTestUpstream(TEST_SERVICE_NAME, servers);

    int excludedServerIndex = 0;

    ServerEntry serverEntry = upstream.acquireServer(singleton(excludedServerIndex));

    assertEquals("b", serverEntry.getAddress());

    assertEquals(0, servers.get(0).getCurrentRequests());
    assertEquals(1, servers.get(1).getCurrentRequests());
  }

  @Test
  public void acquireReleaseWhenMaxFailsIsZero() {
    List<Server> servers = List.of(new Server("a", null, 1, null));

    ApplicationConfig applicationConfig = buildTestConfig();

    UpstreamConfigs configMap = ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT);

    Upstream upstream = createTestUpstream(TEST_SERVICE_NAME, servers, configMap);
    int index = upstream.acquireServer().getIndex();
    upstream.releaseServer(index, false, true, 100);

    int serverIndex = 0;

    assertEquals(serverIndex, upstream.acquireServer().getIndex());
  }

  @Test
  public void acquireReleaseFromTwoThreads() throws Exception {
    int numOfRequests = 100_000;
    int tests = 100;
    int weight = numOfRequests * tests * 2 + 1;
    List<Server> servers = List.of(new Server("a", null, weight, null));

    Upstream upstream = new Upstream(TEST_SERVICE_NAME, configMap, servers, null, false);
    upstream.setStatLimit(weight);
    Server server = servers.get(0);

    Runnable acquireReleaseTask = () -> acquireReleaseUpstream(upstream, numOfRequests);

    for (int t = 1; t <= tests; t++) {
      long start = currentTimeMillis();
      Thread thread = new Thread(acquireReleaseTask);
      thread.start();

      acquireReleaseUpstream(upstream, numOfRequests);

      thread.join();

      assertEquals("current requests", 0, server.getCurrentRequests());

      LOGGER.info("finished iteration {} out of {} in {} ms", t, tests, (currentTimeMillis() - start));
    }

    assertEquals("stats requests", weight - 1, server.getStatsRequests());
  }

  private static void assertServerCounters(List<Server> servers, int serverIndex, int requests, int statsRequests) {

    assertEquals("currentRequests", requests, servers.get(serverIndex).getCurrentRequests());
    assertEquals("statsRequests", statsRequests, servers.get(serverIndex).getStatsRequests());
  }

  private static void acquireReleaseUpstream(Upstream upstream, int times) {
    for (int i = 0; i < times; i++) {
      ServerEntry serverEntry = upstream.acquireServer();
      if (serverEntry != null) {
        upstream.releaseServer(serverEntry.getIndex(), false, false, 100);
      }
    }
  }

  private static Upstream createTestUpstream(String serviceName, List<Server> servers) {
    return createTestUpstream(serviceName, servers, getDefaultConfig());
  }

  private static Upstream createTestUpstream(String serviceName, List<Server> servers, UpstreamConfigs upstreamConfigs) {
    return new Upstream(serviceName, upstreamConfigs, servers, null, false);
  }

  private static List<Server> buildServers() {
    return List.of(new Server("a", null, 1, null), new Server("b", null, 2, null));
  }
}
