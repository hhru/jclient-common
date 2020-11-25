package ru.hh.jclient.consul;

import com.google.common.io.BaseEncoding;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.kv.ImmutableValue;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.option.ConsistencyMode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import ru.hh.jclient.consul.model.ApplicationConfig;
import ru.hh.jclient.consul.model.Host;
import ru.hh.jclient.consul.model.Profile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class UpstreamConfigServiceImplTest {
  private static UpstreamConfigServiceImpl service;
  private static String SERVICE_NAME = "upstream1";
  static List<String> upstreamList = List.of(SERVICE_NAME);
  static Consul consulClient = mock(Consul.class);
  static int watchSeconds = 10;

  @BeforeClass
  public static void init() {
    service = new UpstreamConfigServiceImpl(upstreamList, consulClient, watchSeconds, ConsistencyMode.DEFAULT);
  }

  @Test
  public void testGetConfig() {
    Collection<Value> values = prepareValues();
    Map<String, ApplicationConfig> configMap = service.readValues(values);

    //two apps
    assertEquals(2, configMap.size());
    ApplicationConfig applicationConfig = configMap.get("app-name");
    assertNotNull(applicationConfig);
    Map<String, Host> hosts = applicationConfig.getHosts();
    assertNotNull(hosts);
    Host host = hosts.get("default");
    assertNotNull(host);
    Map<String, Profile> profiles = host.getProfiles();
    assertNotNull(profiles);
    Profile profile = profiles.get("default");
    assertNotNull(profile);

    assertEquals(43, profile.getMaxTries().intValue());
    assertTrue(profile.getRetryPolicy().get(503).isIdempotent());

    //second app
    assertEquals(56, configMap.get("app2").getHosts().get("default").getProfiles().get("default").getMaxTries().intValue());
  }

  @Test
  public void testNotify() {
    List<String> consumerMock = new ArrayList<>();

    try {
      service.setupListener(consumerMock::add);
    } catch (Exception ex) {
      //ignore
    }
    service.notifyListeners();

    assertEquals(1, consumerMock.size());

  }


  private Collection<Value> prepareValues() {
    Collection<Value> values = new ArrayList<>();
    ImmutableValue template = ImmutableValue.builder().key("template").value("template")
            .createIndex(System.currentTimeMillis()).modifyIndex(System.currentTimeMillis())
            .lockIndex(System.currentTimeMillis()).flags(System.currentTimeMillis())
            .build();

    String twoProfiles = "{\"hosts\": {\"default\": {\"profiles\": {\"default\": {\"max_tries\": \"43\"," +
        "\"retry_policy\": {\"599\": {\"idempotent\": \"false\"},\"503\": {\"idempotent\": \"true\"}}},\"" +
        "externalRequestsProfile\": {\"fail_timeout_sec\": \"5\"}}}}}";
    values.add(ImmutableValue.copyOf(template).withKey("upstream/app-name/")
            .withValue(BaseEncoding.base64().encode(twoProfiles.getBytes())));

    String secondAppProfile = "{\"hosts\": {\"default\": {\"profiles\": {\"default\": {\"max_tries\": \"56\"}}}}}";
    values.add(ImmutableValue.copyOf(template).withKey("upstream/app2/")
            .withValue(BaseEncoding.base64().encode(secondAppProfile.getBytes())));
    return values;
  }
}
