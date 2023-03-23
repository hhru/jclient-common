package ru.hh.jclient.consul;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import ru.hh.consul.Consul;
import ru.hh.consul.KeyValueClient;
import ru.hh.consul.config.ClientConfig;
import ru.hh.consul.model.ConsulResponse;
import ru.hh.consul.model.kv.ImmutableValue;
import ru.hh.consul.model.kv.Value;
import ru.hh.consul.monitoring.ClientEventCallback;
import ru.hh.consul.monitoring.ClientEventHandler;
import static ru.hh.consul.option.ConsistencyMode.DEFAULT;
import ru.hh.consul.option.QueryOptions;
import ru.hh.jclient.common.balancing.ConfigStore;
import ru.hh.jclient.common.balancing.ConfigStoreImpl;
import ru.hh.jclient.common.balancing.JClientInfrastructureConfig;
import ru.hh.jclient.common.balancing.UpstreamConfig;
import ru.hh.jclient.common.balancing.UpstreamConfigs;
import ru.hh.jclient.common.balancing.UpstreamManager;
import static ru.hh.jclient.consul.UpstreamConfigServiceConsulConfig.copyOf;

public class UpstreamConfigServiceImplTest {
  private static KeyValueClient keyValueClient = mock(KeyValueClient.class);
  private static String currentServiceName = "CALLER_NAME";
  private static JClientInfrastructureConfig infrastructureConfig = mock(JClientInfrastructureConfig.class);
  private static UpstreamManager upstreamManager = mock(UpstreamManager.class);
  private ConfigStore configStore = new ConfigStoreImpl();
  static Consul consulClient = mock(Consul.class);
  static int watchSeconds = 10;

  private static final ImmutableValue template = ImmutableValue.builder().key("template").value("template")
      .createIndex(System.currentTimeMillis()).modifyIndex(System.currentTimeMillis())
      .lockIndex(System.currentTimeMillis()).flags(System.currentTimeMillis())
      .build();
  public UpstreamConfigServiceConsulConfig configTemplate = new UpstreamConfigServiceConsulConfig()
      .setWatchSeconds(watchSeconds)
      .setConsistencyMode(DEFAULT);

  @BeforeClass
  public static void init() {
    when(keyValueClient.getConfig()).thenReturn(new ClientConfig());
    when(keyValueClient.getEventHandler()).thenReturn(new ClientEventHandler("", mock(ClientEventCallback.class)));
    when(consulClient.keyValueClient()).thenReturn(keyValueClient);
    when(infrastructureConfig.getServiceName()).thenReturn(currentServiceName);
  }

  @Test
  public void testGetConfig() {
    Collection<Value> values = prepareValues();
    when(keyValueClient.getConsulResponseWithValues(anyString(), any(QueryOptions.class))).thenReturn(wrapWithResponse(List.copyOf(values)));

    var service = new UpstreamConfigServiceImpl(
        infrastructureConfig,
        consulClient,
        configStore, upstreamManager,
        copyOf(configTemplate).setUpstreams(List.of("app-name", "app2")),
        List.of()
    );

    UpstreamConfigs profiles = configStore.getUpstreamConfig("app-name");
    assertNotNull(profiles);
    UpstreamConfig profile = profiles.get("default").get();
    assertNotNull(profile);

    assertEquals(43, profile.getMaxTries());
    assertTrue(profile.getRetryPolicy().getRules().get(503));

    //second app
    assertEquals(56, configStore.getUpstreamConfig("app2").get("default").get().getMaxTries());
  }

  @Test
  public void testNoConfig() {
    when(keyValueClient.getConsulResponseWithValues(anyString(), any(QueryOptions.class)))
        .thenReturn(wrapWithResponse(List.of()));
    UpstreamConfigServiceConsulConfig config = copyOf(configTemplate).setUpstreams(List.of("app-name"));
    assertThrows(
        IllegalStateException.class,
        () -> new UpstreamConfigServiceImpl(infrastructureConfig, consulClient, configStore, upstreamManager, config, List.of())
    );
    when(keyValueClient.getConsulResponseWithValues(anyString(), any(QueryOptions.class)))
        .thenReturn(wrapWithResponse(List.copyOf(prepareValues())));
    new UpstreamConfigServiceImpl(infrastructureConfig, consulClient, configStore, upstreamManager, config, List.of());
    assertNotNull(configStore.getUpstreamConfig("app-name"));
  }

  @Test
  public void testBadConfig() {
    String badFormatKey = "badFormat";
    var badFormatValue = ImmutableValue.copyOf(template).withKey(UpstreamConfigServiceImpl.ROOT_PATH + badFormatKey)
        .withValue(new String(Base64.getEncoder().encode("{\"a\":[1,2,3".getBytes())));
    List<Value> values = new ArrayList<>(prepareValues());
    values.add(badFormatValue);
    when(keyValueClient.getConsulResponseWithValues(anyString(), any(QueryOptions.class))).thenReturn(wrapWithResponse(values));
    var ex = assertThrows(
        IllegalStateException.class,
        () -> {
          UpstreamConfigServiceConsulConfig config = copyOf(configTemplate).setUpstreams(List.of("app-name", "badFormat"));
          new UpstreamConfigServiceImpl(infrastructureConfig, consulClient, configStore, upstreamManager, config, List.of());
        }
    );
    assertTrue(ex.getMessage().contains(badFormatKey));
  }


  private Collection<Value> prepareValues() {
    Collection<Value> values = new ArrayList<>();
    String twoProfiles = "{\"hosts\": {\"default\": {\"profiles\": {\"default\": {\"max_tries\": \"43\"," +
        "\"retry_policy\": {\"599\": {\"idempotent\": \"false\"},\"503\": {\"idempotent\": \"true\"}}},\"" +
        "externalRequestsProfile\": {\"fail_timeout_sec\": \"5\"}}}}}";
    values.add(ImmutableValue.copyOf(template).withKey("upstream/app-name/")
        .withValue(new String(Base64.getEncoder().encode(twoProfiles.getBytes()))));

    String secondAppProfile = "{\"hosts\": {\"default\": {\"profiles\": {\"default\": {\"max_tries\": \"56\"}}}}}";
    values.add(ImmutableValue.copyOf(template).withKey("upstream/app2/")
        .withValue(new String(Base64.getEncoder().encode(secondAppProfile.getBytes()))));
    return values;
  }

  private <T> ConsulResponse<T> wrapWithResponse(T value) {
    return new ConsulResponse<>(value, 0, true, BigInteger.ONE, null, null);
  }
}
