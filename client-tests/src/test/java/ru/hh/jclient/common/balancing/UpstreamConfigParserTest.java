package ru.hh.jclient.common.balancing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import ru.hh.jclient.consul.model.ApplicationConfig;
import ru.hh.jclient.consul.model.Host;
import ru.hh.jclient.consul.model.Profile;
import ru.hh.jclient.consul.model.RetryPolicyConfig;

import java.util.HashMap;
import java.util.Map;

public class UpstreamConfigParserTest {
  private static final String DEFAULT = "default";

  @Test
  public void testOneProfile() {

    ApplicationConfig applicationConfig = buildTestConfig();

    Map<String, UpstreamConfig> configMap = UpstreamConfig.fromApplicationConfig(applicationConfig, DEFAULT);
    UpstreamConfig config = configMap.get(DEFAULT);

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

    Map<String, UpstreamConfig> configMap = UpstreamConfig.fromApplicationConfig(applicationConfig, DEFAULT);
    UpstreamConfig config = configMap.get(secondProfileName);

    assertEquals(7, config.getMaxTries());
    assertEquals(8000, config.getRequestTimeoutMs());
  }

  @Test
  public void testDefaultConfig() {
    Map<String, UpstreamConfig> configMap = UpstreamConfig.fromApplicationConfig(new ApplicationConfig(), DEFAULT);
    UpstreamConfig config = configMap.get(DEFAULT);

    assertEquals(UpstreamConfig.DEFAULT_MAX_TRIES, config.getMaxTries());
    assertEquals(UpstreamConfig.DEFAULT_MAX_TIMEOUT_TRIES, config.getMaxTimeoutTries());
    assertEquals(UpstreamConfig.DEFAULT_CONNECT_TIMEOUT_MS, config.getConnectTimeoutMs());
    assertEquals(UpstreamConfig.DEFAULT_REQUEST_TIMEOUT_MS, config.getRequestTimeoutMs());
    assertFalse(config.getRetryPolicy().getRules().get(599));
    assertFalse(config.getRetryPolicy().getRules().get(503));
  }

  @Test
  public void parseRetryPolicy() {
    ApplicationConfig applicationConfig = buildTestConfig();
    Map<Integer, RetryPolicyConfig> retryPolicy = applicationConfig.getHosts().get(DEFAULT).getProfiles().get(DEFAULT).getRetryPolicy();
    retryPolicy.put(503, new RetryPolicyConfig().setIdempotent(true));
    retryPolicy.put(599, new RetryPolicyConfig().setIdempotent(false));

    Map<String, UpstreamConfig> configMap = UpstreamConfig.fromApplicationConfig(applicationConfig, DEFAULT);
    UpstreamConfig config = configMap.get(DEFAULT);

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
