package ru.hh.jclient.common.balancing;

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
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT;
import static ru.hh.jclient.common.balancing.UpstreamConfigParserTest.buildTestConfig;
import ru.hh.jclient.consul.UpstreamConfigService;
import ru.hh.jclient.consul.UpstreamService;
import ru.hh.jclient.consul.model.ApplicationConfig;
import ru.hh.jclient.consul.model.Profile;
import ru.hh.jclient.consul.model.RetryPolicyConfig;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class BalancingUpstreamManagerTest {
  private static final String TEST_BACKEND = "backend";
  private final UpstreamConfigService upstreamConfigService = mock(UpstreamConfigService.class);
  private final UpstreamService upstreamService = mock(UpstreamService.class);

  @Test
  public void createUpstreamManager() {
    ApplicationConfig applicationConfig = buildTestConfig();
    applicationConfig.getHosts().get(DEFAULT).getProfiles().get(DEFAULT);

    when(upstreamConfigService.getUpstreamConfig(TEST_BACKEND)).thenReturn(applicationConfig);

    BalancingUpstreamManager manager = createUpstreamManager(List.of(TEST_BACKEND), 0.5);

    assertEquals(1, manager.getUpstreams().size());

    Upstream upstream =  manager.getUpstream(TEST_BACKEND);

    assertNotNull(upstream.getConfig(DEFAULT).getRetryPolicy().getRules());
    assertFalse(upstream.getConfig(DEFAULT).getRetryPolicy().getRules().get(HttpStatuses.CONNECT_TIMEOUT_ERROR));
    assertFalse(upstream.getConfig(DEFAULT).getRetryPolicy().getRules().get(HttpStatuses.SERVICE_UNAVAILABLE));
  }

  @Test
  public void updateUpstreams() {
    ApplicationConfig applicationConfig = buildTestConfig();
    Profile profile = applicationConfig.getHosts().get(DEFAULT).getProfiles().get(DEFAULT);

    profile.setRetryPolicy(Map.of(599, new RetryPolicyConfig().setIdempotent(false)));

    when(upstreamConfigService.getUpstreamConfig(TEST_BACKEND)).thenReturn(applicationConfig);
    BalancingUpstreamManager manager = createUpstreamManager(List.of(TEST_BACKEND), 0.5);

    manager.updateUpstream(TEST_BACKEND);

    profile.setRetryPolicy(Map.of(
            500, new RetryPolicyConfig().setIdempotent(false),
            503, new RetryPolicyConfig().setIdempotent(true)
        ));
    manager.updateUpstream(TEST_BACKEND);

    Upstream upstream = manager.getUpstream(TEST_BACKEND);

    assertNull(upstream.getConfig(DEFAULT).getRetryPolicy().getRules().get(HttpStatuses.CONNECT_TIMEOUT_ERROR));
    assertTrue(upstream.getConfig(DEFAULT).getRetryPolicy().getRules().get(HttpStatuses.SERVICE_UNAVAILABLE));
    assertFalse(upstream.getConfig(DEFAULT).getRetryPolicy().getRules().get(HttpStatuses.INTERNAL_SERVER_ERROR));
  }

  @Test
  public void ignoreDangerousServerUpdate() {
    ApplicationConfig applicationConfig = buildTestConfig();
    when(upstreamConfigService.getUpstreamConfig(TEST_BACKEND)).thenReturn(applicationConfig);
    List<Server> initialServers = List.of(
            new Server("server1", 100, "test"),
            new Server("server2", 100, "test")
    );
    when(upstreamService.getServers(TEST_BACKEND)).thenReturn(initialServers);
    BalancingUpstreamManager manager = createUpstreamManager(List.of(TEST_BACKEND), 0.0);
    assertEquals(initialServers, manager.getUpstream(TEST_BACKEND).getServers());
    when(upstreamService.getServers(TEST_BACKEND)).thenReturn(List.of(new Server("server3", 100, "test")));
    manager.updateUpstream(TEST_BACKEND);
    assertEquals(initialServers, manager.getUpstream(TEST_BACKEND).getServers());
  }

  @Test
  public void allowNotDangerousServerUpdate() {
    ApplicationConfig applicationConfig = buildTestConfig();
    when(upstreamConfigService.getUpstreamConfig(TEST_BACKEND)).thenReturn(applicationConfig);
    List<Server> initialServers = List.of(
            new Server("server1", 100, "test"),
            new Server("server2", 100, "test")
    );
    when(upstreamService.getServers(TEST_BACKEND)).thenReturn(initialServers);
    BalancingUpstreamManager manager = createUpstreamManager(List.of(TEST_BACKEND), 0.8);
    assertEquals(initialServers, manager.getUpstream(TEST_BACKEND).getServers());
    List<Server> servers = List.of(new Server("server3", 100, "test"));
    when(upstreamService.getServers(TEST_BACKEND)).thenReturn(servers);
    manager.updateUpstream(TEST_BACKEND);
    assertEquals(servers, manager.getUpstream(TEST_BACKEND).getServers());
  }

  @Test
  public void testGetUpstream() {
    when(upstreamConfigService.getUpstreamConfig(TEST_BACKEND)).thenReturn(new ApplicationConfig());

    UpstreamManager upstreamManager = createUpstreamManager(List.of(TEST_BACKEND), 0.5);

    assertNotNull(upstreamManager.getUpstream(TEST_BACKEND));

    assertNotNull(upstreamManager.getUpstream("http://" + TEST_BACKEND));

    assertNotNull(upstreamManager.getUpstream("https://" + TEST_BACKEND));

    assertNull(upstreamManager.getUpstream("missing_upstream"));
  }


  private BalancingUpstreamManager createUpstreamManager(List<String> upstreamList, double allowedDegradationPart) {
    Monitoring monitoring = mock(Monitoring.class);
    return new BalancingUpstreamManager(upstreamList,
            Set.of(monitoring),
            null,
            false,
            upstreamConfigService,
            upstreamService,
            allowedDegradationPart
    );
  }
}
