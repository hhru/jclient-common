package ru.hh.jclient.common.balancing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import ru.hh.jclient.consul.ValueNode;

import java.util.HashMap;
import java.util.Map;

public class UpstreamConfigParserTest {
  private static final String SERVICE_NAME = "backend1";
  private static final String DEFAULT = "default";
  private static final String PROFILE = "profile";

  @Test
  public void testOneProfile() {

    UpstreamConfig config = UpstreamConfig.fromTree(SERVICE_NAME, DEFAULT, DEFAULT, buildValueNode());

    assertEquals(3, config.getMaxTries());
    assertEquals(30, config.getMaxFails());
    assertEquals(2, config.getMaxTimeoutTries());
    assertEquals(1500, config.getFailTimeoutMs());
    assertEquals(200, config.getConnectTimeoutMs());
    assertEquals(2000, config.getRequestTimeoutMs());
  }

  @Test
  public void testTwoProfiles() {
    String profileName = "secondProfile";

    ValueNode rootNode = buildValueNode();
    ValueNode profiles = rootNode
            .getNode(SERVICE_NAME)
            .getNode(DEFAULT)
            .getNode(PROFILE);
    profiles.computeMapIfAbsent("firstProfile");

    ValueNode secondProfile = profiles.computeMapIfAbsent(profileName);

    secondProfile.putValue("max_tries", "7");
    secondProfile.putValue("request_timeout_sec", "8");

    UpstreamConfig config = UpstreamConfig.fromTree(SERVICE_NAME, profileName, DEFAULT, rootNode);

    assertEquals(7, config.getMaxTries());
    assertEquals(8000, config.getRequestTimeoutMs());
  }

  @Test
  public void testDefaultConfig() {
    UpstreamConfig config = UpstreamConfig.fromTree(SERVICE_NAME, DEFAULT, DEFAULT, new ValueNode());


    assertEquals(UpstreamConfig.DEFAULT_MAX_TRIES, config.getMaxTries());
    assertEquals(UpstreamConfig.DEFAULT_MAX_FAILS, config.getMaxFails());
    assertEquals(UpstreamConfig.DEFAULT_MAX_TIMEOUT_TRIES, config.getMaxTimeoutTries());
    assertEquals(UpstreamConfig.DEFAULT_FAIL_TIMEOUT_MS, config.getFailTimeoutMs());
    assertEquals(UpstreamConfig.DEFAULT_CONNECT_TIMEOUT_MS, config.getConnectTimeoutMs());
    assertEquals(UpstreamConfig.DEFAULT_REQUEST_TIMEOUT_MS, config.getRequestTimeoutMs());
    assertFalse(config.getRetryPolicy().getRules().get(599));
    assertFalse(config.getRetryPolicy().getRules().get(503));
  }

  @Test
  public void parseRetryPolicy() {
    ValueNode rootNode = buildValueNode();
    ValueNode profile = rootNode
            .getNode(SERVICE_NAME)
            .getNode(DEFAULT)
            .getNode(PROFILE)
            .getNode(DEFAULT);
    profile.putValue("retry_policy", "timeout,http_503,non_idempotent_503");

    UpstreamConfig config = UpstreamConfig.fromTree(SERVICE_NAME, DEFAULT, DEFAULT, rootNode);

    assertFalse(config.getRetryPolicy().getRules().get(599));
    assertTrue(config.getRetryPolicy().getRules().get(503));
  }

  @Test
  public void parseUnknownRetryPolicy() {
    ValueNode rootNode = buildValueNode();
    ValueNode profile = rootNode
            .getNode(SERVICE_NAME)
            .getNode(DEFAULT)
            .getNode(PROFILE)
            .getNode(DEFAULT);
    profile.putValue("retry_policy", "timeout,unknown");

    UpstreamConfig config = UpstreamConfig.fromTree(SERVICE_NAME, DEFAULT, DEFAULT, rootNode);
    assertFalse(config.getRetryPolicy().getRules().get(599));
    assertNull(config.getRetryPolicy().getRules().get(503));
  }

  private ValueNode buildValueNode() {
    ValueNode rootNode = new ValueNode();
    ValueNode serviceNode = rootNode.computeMapIfAbsent(SERVICE_NAME);
    ValueNode hostNode = serviceNode.computeMapIfAbsent("default");
    ValueNode profileNode = hostNode.computeMapIfAbsent(PROFILE);
    ValueNode defaultProfile = profileNode.computeMapIfAbsent("default");
    defaultProfile.putAll(buildValues());
    return rootNode;
  }

  private Map<String, ValueNode> buildValues() {
    Map<String, ValueNode> values = new HashMap<>();
    values.put("max_tries", new ValueNode("3"));
    values.put("max_timeout_tries", new ValueNode("2"));
    values.put("max_fails", new ValueNode("30"));
    values.put("connect_timeout_sec", new ValueNode("0.2"));
    values.put("request_timeout_sec", new ValueNode("2"));
    values.put("fail_timeout_sec", new ValueNode("1.5"));
    return values;
  }

}
