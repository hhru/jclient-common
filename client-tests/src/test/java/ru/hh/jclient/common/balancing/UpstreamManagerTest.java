package ru.hh.jclient.common.balancing;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.consul.UpstreamConfigService;
import ru.hh.jclient.consul.UpstreamService;
import ru.hh.jclient.consul.ValueNode;

import java.util.List;
import java.util.Set;

public class UpstreamManagerTest {
  private static final String TEST_BACKEND = "backend";
  private static final UpstreamConfigService upstreamConfigService = mock(UpstreamConfigService.class);
  private static final UpstreamService upstreamService = mock(UpstreamService.class);

  @Test
  public void createUpstreamManager() {
    ValueNode rootNode = new ValueNode();
    ValueNode profileNode = buildProfileNode(rootNode);
    profileNode.putValue("max_fails", "5");
    when(upstreamConfigService.getUpstreamConfig()).thenReturn(rootNode);

    UpstreamManager manager = createUpstreamManager(List.of(TEST_BACKEND));

    assertEquals(1, manager.getUpstreams().size());

    Upstream upstream =  manager.getUpstream(TEST_BACKEND);

    assertEquals(5, upstream.getConfig().getMaxFails());

    assertFalse(upstream.getConfig().getRetryPolicy().getRules().get(HttpStatuses.CONNECT_TIMEOUT_ERROR));
    assertFalse(upstream.getConfig().getRetryPolicy().getRules().get(HttpStatuses.SERVICE_UNAVAILABLE));
  }

  @Test
  public void updateUpstreams() {
    ValueNode rootNode = new ValueNode();
    ValueNode profileNode = buildProfileNode(rootNode);
    profileNode.putValue("max_fails", "5");
    profileNode.putValue("retry_policy", "timeout");
    when(upstreamConfigService.getUpstreamConfig()).thenReturn(rootNode);

    UpstreamManager manager = createUpstreamManager(List.of(TEST_BACKEND));

    profileNode.putValue("max_fails", "6");
    manager.updateUpstream(TEST_BACKEND);

    profileNode.putValue("retry_policy", "http_503,non_idempotent_503,http_500");
    manager.updateUpstream(TEST_BACKEND);

    Upstream upstream = manager.getUpstream(TEST_BACKEND);

    assertEquals(6, upstream.getConfig().getMaxFails());

    assertNull(upstream.getConfig().getRetryPolicy().getRules().get(HttpStatuses.CONNECT_TIMEOUT_ERROR));
    assertTrue(upstream.getConfig().getRetryPolicy().getRules().get(HttpStatuses.SERVICE_UNAVAILABLE));
    assertFalse(upstream.getConfig().getRetryPolicy().getRules().get(HttpStatuses.INTERNAL_SERVER_ERROR));

  }

  @Test
  public void testGetUpstream() {
    when(upstreamConfigService.getUpstreamConfig()).thenReturn(new ValueNode());
    UpstreamManager upstreamManager = createUpstreamManager(List.of(TEST_BACKEND));

    assertNotNull(upstreamManager.getUpstream(TEST_BACKEND));

    assertNotNull(upstreamManager.getUpstream("http://" + TEST_BACKEND));

    assertNotNull(upstreamManager.getUpstream("https://" + TEST_BACKEND));

    assertNull(upstreamManager.getUpstream("missing_upstream"));
  }

  private ValueNode buildProfileNode(ValueNode rootNode) {
    return rootNode.computeMapIfAbsent(TEST_BACKEND)
            .computeMapIfAbsent(UpstreamConfig.DEFAULT)
            .computeMapIfAbsent(UpstreamConfig.PROFILE_NODE)
            .computeMapIfAbsent(UpstreamConfig.DEFAULT);
  }

  private static UpstreamManager createUpstreamManager(List<String> upstreamList) {
    Monitoring monitoring = mock(Monitoring.class);
    return new BalancingUpstreamManager(upstreamList,
            newSingleThreadScheduledExecutor(),
            Set.of(monitoring),
            null,
            false,
            upstreamConfigService,
            upstreamService
    );
  }
}
