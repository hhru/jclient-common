package ru.hh.jclient.common.balancing.config;

import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import ru.hh.jclient.common.balancing.UpstreamConfig;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT;
import ru.hh.jclient.common.balancing.UpstreamConfigs;

public class ApplicationConfigTest {

  @Test
  public void testOneProfile() {

    ApplicationConfig applicationConfig = buildTestConfig();

    UpstreamConfigs configMap = ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT);
    UpstreamConfig config = configMap.get(DEFAULT).get();

    assertEquals(3, config.getMaxTries());
    assertEquals(2, config.getMaxTimeoutTries());
    assertEquals(200, config.getConnectTimeoutMs());
    assertEquals(2000, config.getRequestTimeoutMs());
  }

  @Test
  public void testTwoProfiles() {
    String secondProfileName = "secondProfile";
    Profile secondProfile = new Profile()
        .setRequestTimeoutSec(8f)
        .setMaxTries(7);

    ApplicationConfig applicationConfig = buildTestConfig();
    applicationConfig.getHosts().get(DEFAULT).getProfiles().put(secondProfileName, secondProfile);

    UpstreamConfigs configMap = ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT);
    UpstreamConfig config = configMap.get(secondProfileName).get();

    assertEquals(7, config.getMaxTries());
    assertEquals(8000, config.getRequestTimeoutMs());
  }

  @Test
  public void testDefaultConfig() {
    UpstreamConfigs configMap = ApplicationConfig.toUpstreamConfigs(new ApplicationConfig(), DEFAULT);
    UpstreamConfig config = configMap.get(DEFAULT).get();

    assertEquals(UpstreamConfig.DEFAULT_CONFIG.getMaxTries(), config.getMaxTries());
    assertEquals(UpstreamConfig.DEFAULT_CONFIG.getMaxTimeoutTries(), config.getMaxTimeoutTries());
    assertEquals(UpstreamConfig.DEFAULT_CONFIG.getConnectTimeoutMs(), config.getConnectTimeoutMs());
    assertEquals(UpstreamConfig.DEFAULT_CONFIG.getRequestTimeoutMs(), config.getRequestTimeoutMs());
    assertFalse(config.getRetryPolicy().getRules().get(599));
    assertFalse(config.getRetryPolicy().getRules().get(503));
  }

  @Test
  public void parseRetryPolicy() {
    ApplicationConfig applicationConfig = buildTestConfig();
    Map<Integer, RetryPolicyConfig> retryPolicy = applicationConfig.getHosts().get(DEFAULT).getProfiles().get(DEFAULT).getRetryPolicy();
    retryPolicy.put(503, new RetryPolicyConfig().setRetryNonIdempotent(true));
    retryPolicy.put(599, new RetryPolicyConfig().setRetryNonIdempotent(false));

    UpstreamConfigs configMap = ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT);
    UpstreamConfig config = configMap.get(DEFAULT).get();

    assertFalse(config.getRetryPolicy().getRules().get(599));
    assertTrue(config.getRetryPolicy().getRules().get(503));
  }

  public static ApplicationConfig buildTestConfig() {

    Profile profile = new Profile()
        .setConnectTimeoutSec(0.2f)
        .setMaxTries(3)
        .setRequestTimeoutSec(2f)
        .setMaxTimeoutTries(2)
        .setRetryPolicy(new HashMap<>());

    Host host = new Host();
    Map<String, Profile> profiles = new HashMap<>();
    profiles.put(DEFAULT, profile);
    host.setProfiles(profiles);
    Map<String, Host> hostMap = Map.of(DEFAULT, host);
    return new ApplicationConfig().setHosts(hostMap);
  }
}
