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

public class UpstreamManagerTest {
  private static final String TEST_BACKEND = "backend";
  private static final UpstreamConfigService upstreamConfigService = mock(UpstreamConfigService.class);
  private static final UpstreamService upstreamService = mock(UpstreamService.class);

  @Test
  public void createUpstreamManager() {
    ApplicationConfig applicationConfig = buildTestConfig();
    applicationConfig.getHosts().get(DEFAULT).getProfiles().get(DEFAULT)
        .setMaxFails(5);

    when(upstreamConfigService.getUpstreamConfig(TEST_BACKEND)).thenReturn(applicationConfig);

    UpstreamManager manager = createUpstreamManager(List.of(TEST_BACKEND));

    assertEquals(1, manager.getUpstreams().size());

    Upstream upstream =  manager.getUpstream(TEST_BACKEND);

    assertEquals(5, upstream.getConfig().getMaxFails());

    assertNotNull(upstream.getConfig().getRetryPolicy().getRules());
    assertFalse(upstream.getConfig().getRetryPolicy().getRules().get(HttpStatuses.CONNECT_TIMEOUT_ERROR));
    assertFalse(upstream.getConfig().getRetryPolicy().getRules().get(HttpStatuses.SERVICE_UNAVAILABLE));
  }

  @Test
  public void updateUpstreams() {
    ApplicationConfig applicationConfig = buildTestConfig();
    Profile profile = applicationConfig.getHosts().get(DEFAULT).getProfiles().get(DEFAULT);

    profile
        .setMaxFails(5)
        .setRetryPolicy(Map.of(599, new RetryPolicyConfig().setIdempotent(false)));

    when(upstreamConfigService.getUpstreamConfig(TEST_BACKEND)).thenReturn(applicationConfig);
    UpstreamManager manager = createUpstreamManager(List.of(TEST_BACKEND));

    profile.setMaxFails(6);
    manager.updateUpstream(TEST_BACKEND);

    profile.setRetryPolicy(Map.of(
            500, new RetryPolicyConfig().setIdempotent(false),
            503, new RetryPolicyConfig().setIdempotent(true)
        ));
    manager.updateUpstream(TEST_BACKEND);

    Upstream upstream = manager.getUpstream(TEST_BACKEND);

    assertEquals(6, upstream.getConfig().getMaxFails());

    assertNull(upstream.getConfig().getRetryPolicy().getRules().get(HttpStatuses.CONNECT_TIMEOUT_ERROR));
    assertTrue(upstream.getConfig().getRetryPolicy().getRules().get(HttpStatuses.SERVICE_UNAVAILABLE));
    assertFalse(upstream.getConfig().getRetryPolicy().getRules().get(HttpStatuses.INTERNAL_SERVER_ERROR));

  }

  @Test
  public void testGetUpstream() {
    when(upstreamConfigService.getUpstreamConfig(TEST_BACKEND)).thenReturn(new ApplicationConfig());

    UpstreamManager upstreamManager = createUpstreamManager(List.of(TEST_BACKEND));

    assertNotNull(upstreamManager.getUpstream(TEST_BACKEND));

    assertNotNull(upstreamManager.getUpstream("http://" + TEST_BACKEND));

    assertNotNull(upstreamManager.getUpstream("https://" + TEST_BACKEND));

    assertNull(upstreamManager.getUpstream("missing_upstream"));
  }


  private static UpstreamManager createUpstreamManager(List<String> upstreamList) {
    Monitoring monitoring = mock(Monitoring.class);
    return new BalancingUpstreamManager(upstreamList,
            newSingleThreadScheduledExecutor(),
            Set.of(monitoring),
            null,
            false,
            1,
            upstreamConfigService,
            upstreamService
    );
  }
}
