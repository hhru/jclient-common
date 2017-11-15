package ru.hh.jclient.common.balancing;

import static java.util.Collections.singletonMap;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.UpstreamManager;

import java.util.List;

public class UpstreamManagerTest {
  private static final String TEST_BACKEND = "backend";

  @Test
  public void createUpstreamManager() throws Exception {
    UpstreamManager manager = createUpstreamManager(TEST_BACKEND, "max_fails=5 | server=a");

    assertEquals(1, manager.getUpstreams().size());

    Upstream upstream =  manager.getUpstream(TEST_BACKEND);

    assertEquals(5, upstream.getConfig().getMaxFails());
    assertEquals("a", upstream.getConfig().getServers().get(0).getAddress());

    assertTrue(upstream.getConfig().getRetryPolicy().isConnectTimeout());
    assertTrue(upstream.getConfig().getRetryPolicy().isRequestTimeout());
    assertFalse(upstream.getConfig().getRetryPolicy().isNonIdempotentRequestTimeout());
  }

  @Test
  public void updateUpstreams() throws Exception {
    UpstreamManager manager = createUpstreamManager(TEST_BACKEND, "max_fails=5 retry_policy=timeout | server=a | server=b");

    manager.updateUpstream(TEST_BACKEND, "max_fails=6 | server=a");
    manager.updateUpstream(TEST_BACKEND, "max_fails=6 retry_policy=http_503,non_idempotent_503 | server=a | server=c");
    manager.updateUpstream("new_backend", "| server=d");

    Upstream upstream = manager.getUpstream(TEST_BACKEND);
    List<Server> servers = upstream.getConfig().getServers();

    assertEquals(6, upstream.getConfig().getMaxFails());
    assertEquals(2, servers.size());
    assertEquals("a", servers.get(0).getAddress());
    assertEquals("c", servers.get(1).getAddress());

    assertFalse(upstream.getConfig().getRetryPolicy().isConnectTimeout());
    assertTrue(upstream.getConfig().getRetryPolicy().isRequestTimeout());
    assertTrue(upstream.getConfig().getRetryPolicy().isNonIdempotentRequestTimeout());

    upstream = manager.getUpstream("new_backend");

    assertEquals("d", upstream.getConfig().getServers().get(0).getAddress());
  }

  @Test
  public void testRemoveUpstream() throws Exception {
    UpstreamManager manager = createUpstreamManager(TEST_BACKEND, "max_fails=5 | server=a");

    manager.updateUpstream(TEST_BACKEND, null);

    assertNull(manager.getUpstream(TEST_BACKEND));
    assertEquals(0, manager.getUpstreams().size());
  }

  @Test
  public void testGetUpstream() throws Exception {
    UpstreamManager upstreamManager = createUpstreamManager(TEST_BACKEND, "|server=server");

    assertNotNull(upstreamManager.getUpstream(TEST_BACKEND));

    assertNotNull(upstreamManager.getUpstream("http://" + TEST_BACKEND));

    assertNotNull(upstreamManager.getUpstream("https://" + TEST_BACKEND));

    assertNull(upstreamManager.getUpstream("missing_upstream"));
  }

  private static UpstreamManager createUpstreamManager(String backend, String configString) {
    return new BalancingUpstreamManager(singletonMap(backend, configString), newSingleThreadScheduledExecutor(), mock(Monitoring.class));
  }
}
