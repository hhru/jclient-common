package ru.hh.jclient.common.balancing;

import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matchers;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.common.Monitoring;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT;
import ru.hh.jclient.common.balancing.config.ApplicationConfig;
import static ru.hh.jclient.common.balancing.config.ApplicationConfigTest.buildTestConfig;
import ru.hh.jclient.common.balancing.config.Profile;
import ru.hh.jclient.common.balancing.config.RetryPolicyConfig;

public class BalancingUpstreamManagerTest {
  private static final String TEST_BACKEND = "backend";
  private final ConfigStore configStore = new ConfigStoreImpl();
  private final ServerStore serverStore = new ServerStoreImpl();

  @Test
  public void createUpstreamManager() {
    ApplicationConfig applicationConfig = buildTestConfig();
    applicationConfig.getHosts().get(DEFAULT).getProfiles().get(DEFAULT);
    configStore.updateConfig(TEST_BACKEND, ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));

    BalancingUpstreamManager manager = createUpstreamManager(List.of(TEST_BACKEND));

    assertEquals(1, manager.getUpstreams().size());

    Upstream upstream = manager.getUpstream(TEST_BACKEND);

    assertNotNull(upstream.getConfig(DEFAULT).getRetryPolicy().getRules());
    assertFalse(upstream.getConfig(DEFAULT).getRetryPolicy().getRules().get(HttpStatuses.CONNECT_TIMEOUT_ERROR));
    assertFalse(upstream.getConfig(DEFAULT).getRetryPolicy().getRules().get(HttpStatuses.SERVICE_UNAVAILABLE));
  }

  @Test
  public void updateUpstreams() {
    ApplicationConfig applicationConfig = buildTestConfig();
    Profile profile = applicationConfig.getHosts().get(DEFAULT).getProfiles().get(DEFAULT);

    profile.setRetryPolicy(Map.of(599, new RetryPolicyConfig().setIdempotent(false)));

    configStore.updateConfig(TEST_BACKEND, ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));
    BalancingUpstreamManager manager = createUpstreamManager(List.of(TEST_BACKEND));

    manager.updateUpstreams(Set.of(TEST_BACKEND));

    profile.setRetryPolicy(Map.of(
        500, new RetryPolicyConfig().setIdempotent(false),
        503, new RetryPolicyConfig().setIdempotent(true)
    ));
    configStore.updateConfig(TEST_BACKEND, ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));
    manager.updateUpstreams(Set.of(TEST_BACKEND));

    Upstream upstream = manager.getUpstream(TEST_BACKEND);

    assertNull(upstream.getConfig(DEFAULT).getRetryPolicy().getRules().get(HttpStatuses.CONNECT_TIMEOUT_ERROR));
    assertTrue(upstream.getConfig(DEFAULT).getRetryPolicy().getRules().get(HttpStatuses.SERVICE_UNAVAILABLE));
    assertFalse(upstream.getConfig(DEFAULT).getRetryPolicy().getRules().get(HttpStatuses.INTERNAL_SERVER_ERROR));
  }

  @Test
  public void ignoreDangerousServerUpdate() {
    ApplicationConfig applicationConfig = buildTestConfig();
    configStore.updateConfig(TEST_BACKEND, ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));
    List<Server> initialServers = List.of(
        new Server("server1", 100, "test"),
        new Server("server2", 100, "test")
    );
    serverStore.updateServers(TEST_BACKEND, initialServers, List.of());
    BalancingUpstreamManager manager = createUpstreamManager(List.of(TEST_BACKEND));

    assertThat(initialServers, Matchers.containsInAnyOrder(manager.getUpstream(TEST_BACKEND).getServers().toArray()));
    serverStore.updateServers(TEST_BACKEND, List.of(), initialServers);
    manager.updateUpstreams(Set.of(TEST_BACKEND));
    assertThat(initialServers, Matchers.containsInAnyOrder(manager.getUpstream(TEST_BACKEND).getServers().toArray()));
  }

  @Test
  public void allowNotDangerousServerUpdate() {
    ApplicationConfig applicationConfig = buildTestConfig();
    configStore.updateConfig(TEST_BACKEND, ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));
    List<Server> initialServers = List.of(
        new Server("server1", 100, "test"),
        new Server("server2", 100, "test")
    );
    serverStore.updateServers(TEST_BACKEND, initialServers, List.of());
    BalancingUpstreamManager manager = createUpstreamManager(List.of(TEST_BACKEND));
    assertEquals(initialServers, manager.getUpstream(TEST_BACKEND).getServers());
    List<Server> servers = List.of(new Server("server3", 100, "test"));
    serverStore.updateServers(TEST_BACKEND, servers, initialServers);
    manager.updateUpstreams(Set.of(TEST_BACKEND));
    assertEquals(servers, manager.getUpstream(TEST_BACKEND).getServers());
  }

  @Test
  public void skipUpdateIfNoConfigs() {
    List<Server> initialServers = List.of(
        new Server("server1", 100, "test"),
        new Server("server2", 100, "test")
    );
    serverStore.updateServers(TEST_BACKEND, initialServers, List.of());
    BalancingUpstreamManager manager = createUpstreamManager(List.of(TEST_BACKEND));
    manager.updateUpstreams(Set.of(TEST_BACKEND));
    assertNull(manager.getUpstream(TEST_BACKEND));
    ApplicationConfig applicationConfig = buildTestConfig();
    configStore.updateConfig(TEST_BACKEND, ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));
    manager.updateUpstreams(Set.of(TEST_BACKEND));
    assertThat(initialServers, Matchers.containsInAnyOrder(manager.getUpstream(TEST_BACKEND).getServers().toArray()));
  }

  @Test
  public void testGetUpstream() {
    configStore.updateConfig(TEST_BACKEND, ApplicationConfig.toUpstreamConfigs(new ApplicationConfig(), DEFAULT));

    UpstreamManager upstreamManager = createUpstreamManager(List.of(TEST_BACKEND));

    assertNotNull(upstreamManager.getUpstream(TEST_BACKEND));

    assertNotNull(upstreamManager.getUpstream("http://" + TEST_BACKEND));

    assertNotNull(upstreamManager.getUpstream("https://" + TEST_BACKEND));

    assertNull(upstreamManager.getUpstream("missing_upstream"));
  }

  private BalancingUpstreamManager createUpstreamManager(List<String> upstreamList) {
    Monitoring monitoring = mock(Monitoring.class);
    JClientInfrastructureConfig infrastructureConfig = mock(JClientInfrastructureConfig.class);
    BalancingUpstreamManager balancingUpstreamManager = new BalancingUpstreamManager(
        configStore,
        serverStore,
        Set.of(monitoring),
        infrastructureConfig,
        false
    );
    balancingUpstreamManager.updateUpstreams(upstreamList);
    return balancingUpstreamManager;
  }
}
