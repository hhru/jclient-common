package ru.hh.jclient.common.balancing;

import static java.util.Collections.singletonMap;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.UpstreamManager;

public class UpstreamManagerTest {
  private static final String TEST_BACKEND = "backend";

  @Test
  public void createUpstreamManager() throws Exception {
    UpstreamManager manager = createUpstreamManager(TEST_BACKEND, "max_fails=5 | server=a");

    assertEquals(1, manager.getUpstreams().size());

    Upstream upstream =  manager.getUpstream(TEST_BACKEND);

    assertEquals(5, upstream.getConfig().getMaxFails());
    assertEquals("a", upstream.getServerAddress(0));
  }

  @Test
  public void updateUpstreams() throws Exception {
    UpstreamManager manager = createUpstreamManager(TEST_BACKEND, "max_fails=5 | server=a | server=b");

    manager.updateUpstream(TEST_BACKEND, "max_fails=6 | server=a");
    manager.updateUpstream(TEST_BACKEND, "max_fails=6 | server=a | server=c");
    manager.updateUpstream("new_backend", "| server=d");

    Upstream upstream = manager.getUpstream(TEST_BACKEND);

    assertEquals(6, upstream.getConfig().getMaxFails());
    assertEquals(2, upstream.getConfig().getServers().size());
    assertEquals("a", upstream.getServerAddress(0));
    assertEquals("c", upstream.getServerAddress(1));

    upstream = manager.getUpstream("new_backend");

    assertEquals("d", upstream.getServerAddress(0));
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
