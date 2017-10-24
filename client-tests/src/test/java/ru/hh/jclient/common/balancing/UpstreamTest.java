package ru.hh.jclient.common.balancing;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static ru.hh.jclient.common.balancing.ServerCounterTest.assertCounter;

import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;

public class UpstreamTest {
  private static final String TEST_HOST = "backend";
  private static final String TEST_CONFIG = "| server=a weight=1 | server=b weight=2";

  @Test
  public void createUpstream() throws Exception {
    Upstream upstream = createTestUpstream(TEST_HOST,"| server=a");

    assertEquals(TEST_HOST, upstream.getName());

    ServerCounter counter = upstream.getConfig().getServer(0).get().getCounter();
    assertEquals(0, counter.getRequests());
    assertEquals(0, counter.getFails());
  }

  @Test
  public void getServerAddress() throws Exception {
    Upstream upstream = createTestUpstream(TEST_HOST, TEST_CONFIG);

    assertEquals("a", upstream.getServerAddress(0));
    assertEquals("b", upstream.getServerAddress(1));
  }

  @Test
  public void acquireServer() throws Exception {
    Upstream upstream = createTestUpstream(TEST_HOST, TEST_CONFIG);

    int last = upstream.acquireServer();
    assertEquals("a", upstream.getServerAddress(last));

    last = upstream.acquireServer();
    assertEquals("b", upstream.getServerAddress(last));

    last = upstream.acquireServer();
    assertEquals("b", upstream.getServerAddress(last));

    last = upstream.acquireServer();
    assertEquals("b", upstream.getServerAddress(last));

    last = upstream.acquireServer();
    assertEquals("a", upstream.getServerAddress(last));

    assertEquals(2, upstream.getServerCounter(0).getRequests());
    assertEquals(3, upstream.getServerCounter(1).getRequests());
  }

  @Test
  public void acquireServerWithExcluding() throws Exception {
    Upstream upstream = createTestUpstream(TEST_HOST, TEST_CONFIG);

    int indexOfServerA = 0;

    int last = upstream.acquireServer(Collections.singleton(indexOfServerA));

    assertEquals("b", upstream.getServerAddress(last));

    assertEquals(0, upstream.getServerCounter(0).getRequests());
    assertEquals(1, upstream.getServerCounter(1).getRequests());
  }

  @Test
  public void acquireAndReleaseServer() throws Exception {
    Upstream upstream = createTestUpstream(TEST_HOST, TEST_CONFIG);

    upstream.acquireServer(); // a
    int last = upstream.acquireServer(); // b

    upstream.releaseServer(last, false); // b

    last = upstream.acquireServer();
    assertEquals("b", upstream.getServerAddress(last));

    ServerCounter counter = upstream.getServerCounter(0);
    assertEquals(1, counter.getRequests());
    assertEquals(0, counter.getFails());

    counter = upstream.getServerCounter(1);
    assertEquals(1, counter.getRequests());
    assertEquals(0, counter.getFails());
  }

  @Test
  public void acquireReleaseServerWithFails() throws Exception {
    Upstream upstream = createTestUpstream(TEST_HOST, "max_fails=1 fail_timeout_sec=0.1 | server=a");

    int serverIndex = 0;
    assertEquals(0, upstream.getServerCounter(serverIndex).getFails());

    assertEquals(serverIndex, upstream.acquireServer());
    upstream.releaseServer(serverIndex, true);

    assertFalse(upstream.getConfig().getServers().get(serverIndex).isActive());
    assertEquals(1, upstream.getServerCounter(serverIndex).getFails());
    assertEquals(-1, upstream.acquireServer());
  }

  @Test
  public void acquireReleaseWhenMaxFailsIsZero() throws Exception {
    Upstream upstream = createTestUpstream(TEST_HOST, "max_fails=0 fail_timeout_sec=0.1 | server=a");

    upstream.releaseServer(upstream.acquireServer(), true);

    int serverIndex = 0;

    assertEquals(serverIndex, upstream.acquireServer());
  }

  @Test
  public void acquireReleaseFromTwoThreads() throws Exception {
    Upstream upstream = createTestUpstream(TEST_HOST, "max_fails=2 fail_timeout_sec=0.1 | server=a");

    int numOfRequests = 100_000;
    Runnable acquireReleaseTask = () -> acquireReleaseUpstream(upstream, numOfRequests);

    int tests = 100;
    for (int t = 1; t <= tests; t++) {
      long start = currentTimeMillis();
      Thread thread = new Thread(acquireReleaseTask);
      thread.start();

      acquireReleaseUpstream(upstream, numOfRequests);

      thread.join();

      ServerCounter counter = upstream.getServerCounter(0);
      assertCounter(counter, 0, 0, numOfRequests * 2, 0);

      System.out.println("finished iteration " + t + " out of " + tests + " in " + (currentTimeMillis() - start) + " ms");
    }
  }

  private static void acquireReleaseUpstream(Upstream upstream, int times) {
    for (int i = 0; i < times; i++) {
      int index = upstream.acquireServer();
      upstream.releaseServer(index, false);
    }
  }

  static Upstream createTestUpstream(String host, String configStr) {
    UpstreamConfig config = UpstreamConfig.parse(configStr);
    return new Upstream(host, config, mock(ScheduledExecutorService.class));
  }
}
