package ru.hh.jclient.consul;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import ru.hh.jclient.common.balancing.config.BalancingStrategyType;
import static ru.hh.jclient.consul.UpstreamConfigServiceConsulConfig.copyOf;

@SuppressWarnings("resource")
public class UpstreamConfigServiceImplTest {
  private static KeyValueClient keyValueClient = mock(KeyValueClient.class);
  private static String currentServiceName = "CALLER_NAME";
  private static JClientInfrastructureConfig infrastructureConfig = mock(JClientInfrastructureConfig.class);
  private static UpstreamManager upstreamManager = mock(UpstreamManager.class);
  private ConfigStore configStore = new ConfigStoreImpl();
  static Consul consulClient = mock(Consul.class);
  static int watchSeconds = 10;

  private static final ImmutableValue template = ImmutableValue
      .builder()
      .key("template")
      .value("template")
      .createIndex(System.currentTimeMillis())
      .modifyIndex(System.currentTimeMillis())
      .lockIndex(System.currentTimeMillis())
      .flags(System.currentTimeMillis())
      .build();
  public UpstreamConfigServiceConsulConfig configTemplate = new UpstreamConfigServiceConsulConfig()
      .setWatchSeconds(watchSeconds)
      .setConsistencyMode(DEFAULT);

  @BeforeAll
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

    new UpstreamConfigServiceImpl(
        infrastructureConfig,
        consulClient,
        configStore, upstreamManager,
        copyOf(configTemplate).setUpstreams(List.of("app-name", "app2")),
        List.of()
    );

    UpstreamConfigs upstream = configStore.getUpstreamConfig("app-name");
    assertNotNull(upstream);
    assertEquals(BalancingStrategyType.ADAPTIVE, upstream.getBalancingStrategyType());
    UpstreamConfig profile = upstream.get("default").get();
    assertNotNull(profile);

    assertEquals(43, profile.getMaxTries());
    assertEquals(
        Map.of(
            503, true,
            599, false,
            502, false,
            504, true
        ),
        profile.getRetryPolicy().getRules()
    );

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
    var badFormatValue = ImmutableValue
        .copyOf(template)
        .withKey(UpstreamConfigServiceImpl.ROOT_PATH + badFormatKey)
        .withValue(encodeBase64("{\"a\":[1,2,3"));
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

  @Test
  public void testBalancingStrategyParsing() {
    testBalancingStrategyParsing("weighted", BalancingStrategyType.WEIGHTED);
    testBalancingStrategyParsing("adaptive", BalancingStrategyType.ADAPTIVE);

    testBalancingStrategyParsing("ADAPTIVE", BalancingStrategyType.WEIGHTED); // we accept only lower case -> fallback to default
    testBalancingStrategyParsing("foo_asd", BalancingStrategyType.WEIGHTED); // unknown value -> fallback to default
    testBalancingStrategyParsing("", BalancingStrategyType.WEIGHTED); // empty value is also unknown -> fallback to default
  }

  private void testBalancingStrategyParsing(String balancingStrategy, BalancingStrategyType expectedBalancingStrategyType) {
    String upstreamConfig = String.format(
        """
            {
                "balancing_strategy": "%s",
                "hosts": {
                    "default": {
                        "profiles": {
                            "default": {
                            }
                        }
                    }
                }
            }
            """,
        balancingStrategy
    );

    UpstreamConfigs upstream = parseUpstreamConfig(upstreamConfig);
    assertEquals(expectedBalancingStrategyType, upstream.getBalancingStrategyType());
  }

  @Test
  public void testMissingBalancingStrategyParsing() {
    String upstreamConfig = """
        {
            "hosts": {
                "default": {
                    "profiles": {
                        "default": {
                        }
                    }
                }
            }
        }
        """;

    UpstreamConfigs upstream = parseUpstreamConfig(upstreamConfig);
    assertEquals(BalancingStrategyType.WEIGHTED, upstream.getBalancingStrategyType()); // balancing strategy not specified -> use default
  }

  private UpstreamConfigs parseUpstreamConfig(String upstreamConfig) {
    String upstreamName = "app-name" + ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);

    Value consulValue = ImmutableValue
        .copyOf(template)
        .withKey("upstream/%s/".formatted(upstreamName))
        .withValue(encodeBase64(upstreamConfig));

    when(keyValueClient.getConsulResponseWithValues(anyString(), any(QueryOptions.class))).thenReturn(wrapWithResponse(List.of(consulValue)));

    new UpstreamConfigServiceImpl(
        infrastructureConfig,
        consulClient,
        configStore,
        upstreamManager,
        copyOf(configTemplate).setUpstreams(List.of(upstreamName)),
        List.of()
    );

    return configStore.getUpstreamConfig(upstreamName);
  }

  private Collection<Value> prepareValues() {
    Collection<Value> values = new ArrayList<>();
    String twoProfiles = """
        {
            "hosts": {
                "default": {
                    "profiles": {
                        "default": {
                            "max_tries": "43",
                            "retry_policy": {
                                "599": {
                                    "retry_non_idempotent": "false"
                                },
                                "503": {
                                    "retry_non_idempotent": "true"
                                },
                                "502": {
                                    "idempotent": "true"
                                },
                                "504": {
                                    "idempotent": "false",
                                    "retry_non_idempotent": "true"
                                }
                            }
                        },
                        "externalRequestsProfile": {
                            "fail_timeout_sec": "5"
                        }
                    }
                }
            },
            "balancing_strategy": "adaptive"
        }
        """;
    values.add(
        ImmutableValue
            .copyOf(template)
            .withKey("upstream/app-name/")
            .withValue(encodeBase64(twoProfiles))
    );

    String secondAppProfile = "{\"hosts\": {\"default\": {\"profiles\": {\"default\": {\"max_tries\": \"56\"}}}}}";
    values.add(
        ImmutableValue
            .copyOf(template)
            .withKey("upstream/app2/")
            .withValue(encodeBase64(secondAppProfile))
    );
    return values;
  }

  private static String encodeBase64(String str) {
    return Base64.getEncoder().encodeToString(str.getBytes());
  }

  private <T> ConsulResponse<T> wrapWithResponse(T value) {
    return new ConsulResponse<>(value, 0, true, BigInteger.ONE, null, null);
  }
}
