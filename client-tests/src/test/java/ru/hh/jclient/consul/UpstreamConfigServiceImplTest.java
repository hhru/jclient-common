package ru.hh.jclient.consul;

import com.google.common.io.BaseEncoding;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import ru.hh.consul.Consul;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.model.kv.ImmutableValue;
import ru.hh.consul.model.kv.Value;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static ru.hh.consul.option.ConsistencyMode.DEFAULT;
import ru.hh.consul.option.QueryOptions;
import ru.hh.jclient.consul.model.ApplicationConfig;
import ru.hh.jclient.consul.model.Host;
import ru.hh.jclient.consul.model.Profile;
import ru.hh.jclient.consul.model.config.UpstreamConfigServiceConsulConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class UpstreamConfigServiceImplTest {
  private static KeyValueClient keyValueClient = mock(KeyValueClient.class);
  static Consul consulClient = mock(Consul.class);
  static int watchSeconds = 10;

  private static final ImmutableValue template = ImmutableValue.builder().key("template").value("template")
          .createIndex(System.currentTimeMillis()).modifyIndex(System.currentTimeMillis())
          .lockIndex(System.currentTimeMillis()).flags(System.currentTimeMillis())
          .build();
  public UpstreamConfigServiceConsulConfig defaultConfig = new UpstreamConfigServiceConsulConfig()
      .setWatchSeconds(watchSeconds)
      .setConsistencyMode(DEFAULT);

  @BeforeClass
  public static void init() {
    when(consulClient.keyValueClient()).thenReturn(keyValueClient);

  }

  @Test
  public void testGetConfig() {
    Collection<Value> values = prepareValues();
    when(keyValueClient.getValues(anyString(), any(QueryOptions.class))).thenReturn(List.copyOf(values));

    var service = new UpstreamConfigServiceImpl(List.of("app-name", "app2"), consulClient, defaultConfig);

    ApplicationConfig applicationConfig = service.getUpstreamConfig("app-name");
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
    assertEquals(56,
      service.getUpstreamConfig("app2").getHosts().get("default").getProfiles().get("default").getMaxTries().intValue()
    );
  }

  @Test
  public void testNotify() {
    var service = new UpstreamConfigServiceImpl(List.of("test"), consulClient, defaultConfig.setSyncUpdate(false));
    List<String> consumerMock = new ArrayList<>();

    try {
      service.setupListener(consumerMock::add);
    } catch (Exception ex) {
      //ignore
    }
    service.notifyListeners();

    assertEquals(1, consumerMock.size());

  }

  @Test
  public void testNoConfig() {
    assertThrows(IllegalStateException.class, () -> new UpstreamConfigServiceImpl(List.of("app-name"), consulClient, defaultConfig));
    when(keyValueClient.getValues(anyString(), any(QueryOptions.class))).thenReturn(List.copyOf(prepareValues()));
    var service = new UpstreamConfigServiceImpl(List.of("app-name"), consulClient, defaultConfig);
    assertNotNull(service.getUpstreamConfig("app-name"));
  }

  @Test
  public void testBadConfig() {
    String badFormatKey = "badFormat";
    var badFormatValue = ImmutableValue.copyOf(template).withKey(UpstreamConfigServiceImpl.ROOT_PATH + badFormatKey)
            .withValue(BaseEncoding.base64().encode("{\"a\":[1,2,3".getBytes()));
    List<Value> values = new ArrayList<>(prepareValues());
    values.add(badFormatValue);
    when(keyValueClient.getValues(anyString(), any(QueryOptions.class))).thenReturn(values);
    var ex = assertThrows(
      IllegalStateException.class,
      () -> new UpstreamConfigServiceImpl(List.of("app-name", "badFormat"), consulClient, defaultConfig)
    );
    assertTrue(ex.getMessage().contains(badFormatKey));
  }


  private Collection<Value> prepareValues() {
    Collection<Value> values = new ArrayList<>();
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
