package ru.hh.jclient.common.balancing.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT;

import org.junit.Test;
import ru.hh.jclient.common.balancing.UpstreamConfig;
import ru.hh.jclient.common.balancing.UpstreamConfigs;

import java.util.HashMap;
import java.util.Map;

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
        .setRequestTimeoutMs(8f)
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
    retryPolicy.put(503, new RetryPolicyConfig().setIdempotent(true));
    retryPolicy.put(599, new RetryPolicyConfig().setIdempotent(false));

    UpstreamConfigs configMap = ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT);
    UpstreamConfig config = configMap.get(DEFAULT).get();

    assertFalse(config.getRetryPolicy().getRules().get(599));
    assertTrue(config.getRetryPolicy().getRules().get(503));
  }

  public static ApplicationConfig buildTestConfig() {

    Profile profile = new Profile()
        .setConnectTimeoutMs(0.2f)
        .setMaxTries(3)
        .setRequestTimeoutMs(2f)
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
