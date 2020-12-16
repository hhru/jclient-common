package ru.hh.jclient.common.balancing;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

public class UpstreamTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamTest.class);
  private static final String TEST_HOST = "backend";
  private static final String TEST_HOST_CUSTOM_PROFILE = "foo";
  private static final String TEST_CONFIG = "| server=a weight=1 | server=b weight=2";

  @Test
  public void createUpstreamServiceOnly() {
    Upstream upstream = createTestUpstream(TEST_HOST, TEST_CONFIG);

    assertEquals(TEST_HOST, upstream.getName());

    assertServerCounters(upstream, 0, 0, 0, 0);
    assertServerCounters(upstream, 1, 0, 0, 0);
  }

  @Test
  public void createUpstreamFull() {
    Upstream.UpstreamKey upstreamKey = new Upstream.UpstreamKey(TEST_HOST, TEST_HOST_CUSTOM_PROFILE);
    Upstream upstream = createTestUpstream(String.join(":", TEST_HOST, TEST_HOST_CUSTOM_PROFILE), TEST_CONFIG);

    assertEquals(upstreamKey, upstream.getKey());

    assertServerCounters(upstream, 0, 0, 0, 0);
    assertServerCounters(upstream, 1, 0, 0, 0);
  }

  @Test
  public void acquireServer() {
    Upstream upstream = createTestUpstream(TEST_HOST, "| server=a weight=1 | server=b weight=2");

    assertEquals("a", upstream.acquireServer().getAddress());
    assertServerCounters(upstream, 0, 1, 1, 0);
    assertServerCounters(upstream, 1, 0, 0, 0);

    assertEquals("b", upstream.acquireServer().getAddress());
    assertServerCounters(upstream, 1, 1, 1, 0);

    assertEquals("b", upstream.acquireServer().getAddress());
    assertServerCounters(upstream, 1, 2, 2, 0);

    upstream.releaseServer(0, false, 100);
    upstream.releaseServer(1, false, 100);

    assertServerCounters(upstream, 0, 0, 0, 0);
    assertServerCounters(upstream, 1, 1, 0, 0);

    upstream.releaseServer(1, false, 100);

    assertServerCounters(upstream, 0, 0, 0, 0);
    assertServerCounters(upstream, 1, 0, 0, 0);
  }

  @Test
  public void acquireInactiveServer() {
    Upstream upstream = createTestUpstream(TEST_HOST, TEST_CONFIG);

    upstream.getConfig().getServers().forEach(server -> server.deactivate(1, mock(ScheduledExecutorService.class)));

    assertNull(upstream.acquireServer());

    assertServerCounters(upstream, 0, 0, 0, 0);
    assertServerCounters(upstream, 1, 0, 0, 0);

    upstream.getConfig().getServers().get(1).activate();

    assertEquals("b", upstream.acquireServer().getAddress());

    assertServerCounters(upstream, 1, 1, 1, 0);
  }

  @Test
  public void acquireExcludedServer() {
    Upstream upstream = createTestUpstream(TEST_HOST, TEST_CONFIG);

    int excludedServerIndex = 0;

    ServerEntry serverEntry = upstream.acquireServer(singleton(excludedServerIndex));

    assertEquals("b", serverEntry.getAddress());

    List<Server> servers = upstream.getConfig().getServers();
    assertEquals(0, servers.get(0).getRequests());
    assertEquals(1, servers.get(1).getRequests());
  }

  @Test
  public void oneServerAlwaysAlive() {
    Upstream upstream = createTestUpstream(TEST_HOST, "max_fails=1 fail_timeout_sec=0.1 | server=a");

    int serverIndex = 0;
    Server server = upstream.getConfig().getServers().get(serverIndex);
    assertEquals(0, server.getFails());

    assertEquals(serverIndex, upstream.acquireServer().getIndex());
    upstream.releaseServer(serverIndex, true, 100);

    assertTrue(server.isActive());

    assertEquals(1, server.getFails());
  }

  @Test
  public void acquireReleaseWhenMaxFailsIsZero() {
    Upstream upstream = createTestUpstream(TEST_HOST, "max_fails=0 fail_timeout_sec=0.1 | server=a");

    upstream.releaseServer(upstream.acquireServer().getIndex(), true, 100);

    int serverIndex = 0;

    assertEquals(serverIndex, upstream.acquireServer().getIndex());
  }

  @Test
  public void acquireReleaseWithDeletedServers() {
    UpstreamConfig config = UpstreamConfig.parse("| server=a | server=b | server=c");
    Upstream upstream = new Upstream(TEST_HOST, config, mock(ScheduledExecutorService.class));

    ServerEntry serverA = upstream.acquireServer();
    ServerEntry serverB = upstream.acquireServer();
    ServerEntry serverC = upstream.acquireServer();

    upstream.updateConfig(UpstreamConfig.parse("| server=b | server=c | server=d"));

    upstream.releaseServer(serverA.getIndex(), false, 1);
    upstream.releaseServer(serverB.getIndex(), false, 1);

    assertEquals(0, getServerByAddress(config, "b").getRequests());
    assertEquals(1, getServerByAddress(config, "c").getRequests());
    assertEquals(0, getServerByAddress(config, "d").getRequests());

    upstream.releaseServer(serverC.getIndex(), false, 1);

    assertEquals(0, getServerByAddress(config, "b").getRequests());
    assertEquals(0, getServerByAddress(config, "c").getRequests());
    assertEquals(0, getServerByAddress(config, "d").getRequests());
  }

  @Test
  public void acquireReleaseFromTwoThreads() throws Exception {
    int numOfRequests = 100_000;
    int tests = 100;
    int weight = numOfRequests * tests * 2 + 1;

    Upstream upstream = createTestUpstream(TEST_HOST, "max_fails=2 fail_timeout_sec=0.1 | server=a weight=" + weight);
    Server server = upstream.getConfig().getServers().get(0);

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

  private static void assertServerCounters(Upstream upstream, int serverIndex, int requests, int statsRequests, int fails) {
    List<Server> servers = upstream.getConfig().getServers();

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

  private static Upstream createTestUpstream(String host, String configStr) {
    UpstreamConfig config = UpstreamConfig.parse(configStr);
    return new Upstream(host, config, mock(ScheduledExecutorService.class));
  }

  private static Server getServerByAddress(UpstreamConfig config, String address) {
    return config.getServers().stream().filter(Objects::nonNull).filter(server -> server.getAddress().equals(address)).findFirst().get();
  }
}
