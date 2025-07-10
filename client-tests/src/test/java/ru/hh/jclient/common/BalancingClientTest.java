package ru.hh.jclient.common;

import java.time.Clock;
import java.util.ArrayList;
import static java.util.Collections.singletonList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;
import org.asynchttpclient.Request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import ru.hh.jclient.common.HttpClientImpl.CompletionHandler;
import static ru.hh.jclient.common.TestEventListener.Call.FINISHED;
import static ru.hh.jclient.common.TestEventListener.Call.REQUEST;
import static ru.hh.jclient.common.TestEventListener.Call.RESPONSE;
import ru.hh.jclient.common.balancing.ExternalUrlRequestor;
import ru.hh.jclient.common.balancing.RequestBalancerBuilder;
import ru.hh.jclient.common.balancing.Server;
import ru.hh.jclient.common.balancing.UpstreamConfig;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT;
import ru.hh.jclient.common.balancing.config.ApplicationConfig;
import ru.hh.jclient.common.balancing.config.BalancingStrategyType;
import ru.hh.jclient.common.balancing.config.Host;
import ru.hh.jclient.common.balancing.config.Profile;

public class BalancingClientTest extends BalancingClientTestBase {

  @Test
  public void testBalancing() throws Exception {
    Server server1 = new Server("server1", null, 10, null);
    Server server2 = new Server("server1", null, 5, null);
    Server server3 = new Server("server1", null, 1, null);
    List<Server> servers = List.of(
        server1,
        server2,
        server3
    );
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    ApplicationConfig applicationConfig = buildTestConfig();

    when(configStore.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));

    createHttpClientFactory(List.of(TEST_UPSTREAM));

    int size = 10;

    when(httpClient.executeRequest(any(Request.class), any(CompletionHandler.class)))
        .then(iom -> {
          completeWith(200, iom);
          return null;
        });

    for (int i = 0; i < size; i++) {
      getTestClient().get();
    }

    assertEquals(0, servers.get(0).getCurrentRequests());
    assertEquals(6, servers.get(0).getStatsRequests());

    assertEquals(0, servers.get(1).getCurrentRequests());
    assertEquals(3, servers.get(1).getStatsRequests());

    assertEquals(0, servers.get(2).getCurrentRequests());
    assertEquals(1, servers.get(2).getStatsRequests());
  }

  @Test
  public void testBalancingCrossDc() throws Exception {
    String currentDC = "DC1";
    Server server1 = new Server("server1", null, 11, currentDC);
    Server server2 = new Server("server2", null, 5, "DC2");
    Server server3 = new Server("server3", null, 1, null);
    List<Server> servers = List.of(
        server1,
        server2,
        server3
    );
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    ApplicationConfig applicationConfig = buildTestConfig();
    when(configStore.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));

    createHttpClientFactory(List.of(TEST_UPSTREAM), currentDC, true);

    when(httpClient.executeRequest(any(Request.class), any(CompletionHandler.class)))
        .then(iom -> {
          completeWith(200, iom);
          return null;
        });
    // to avoid rescaling
    for (int i = 0; i < servers.stream().mapToInt(Server::getWeight).max().getAsInt() - 1; i++) {
      getTestClient().get();
    }
    assertEquals(10, servers.get(0).getStatsRequests());
    assertEquals(0, servers.get(1).getStatsRequests());
    assertEquals(0, servers.get(2).getStatsRequests());

    List<Server> noCurrentDcServers = List.of(server2, server3);
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(noCurrentDcServers);
    upstreamManager.updateUpstreams(Set.of(TEST_UPSTREAM));

    for (int i = 0; i < noCurrentDcServers.stream().mapToInt(Server::getWeight).max().getAsInt() - 1; i++) {
      getTestClient().get();
    }

    assertEquals(3, noCurrentDcServers.get(0).getStatsRequests());
    assertEquals(1, noCurrentDcServers.get(1).getStatsRequests());
  }

  @Test
  public void testAddServer() throws Exception {
    Server existingServer = new Server("server1", null, 3, null);
    List<Server> servers = new ArrayList<>();
    servers.add(existingServer);
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    ApplicationConfig applicationConfig = buildTestConfig();

    when(configStore.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));

    createHttpClientFactory(List.of(TEST_UPSTREAM));
    var calledAddresses = new ArrayList<>();
    when(httpClient.executeRequest(any(Request.class), any(CompletionHandler.class))).then(iom -> {
      Request request = completeWith(200, iom);
      calledAddresses.add(request.getUri().getHost());
      return null;
    });
    // less calls than weight, otherwise statRequests is rescaled
    getTestClient().get();
    Server newServer = new Server("server2", null, 3, null);
    servers.add(newServer);
    getTestClient().get();
    assertEquals(2, servers.get(0).getStatsRequests());
    assertNotEquals(newServer.getAddress(), calledAddresses.get(calledAddresses.size() - 1));
    assertEquals(1, servers.get(1).getStatsRequests());
    getTestClient().get();
    assertEquals(newServer.getAddress(), calledAddresses.get(calledAddresses.size() - 1));
    assertEquals(2, servers.get(1).getStatsRequests());
    assertEquals(2, servers.get(0).getStatsRequests());
  }

  @Test
  public void testNoSlowStart() throws Exception {
    Server server1 = new Server("server1", null, 3, null);
    Server server2 = new Server("server2", null, 3, null);
    List<Server> servers = List.of(server1, server2);
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    ApplicationConfig applicationConfig = buildTestConfig();

    when(configStore.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));

    createHttpClientFactory(List.of(TEST_UPSTREAM));
    var calledAddresses = new ArrayList<>();
    when(httpClient.executeRequest(any(Request.class), any(CompletionHandler.class))).then(iom -> {
      Request request = completeWith(200, iom);
      calledAddresses.add(request.getUri().getHost());
      return null;
    });
    getTestClient().get();
    getTestClient().get();
    getTestClient().get();
    assertEquals(2, server1.getStatsRequests());
    assertEquals(1, server2.getStatsRequests());
  }

  @Test
  public void testSlowStart() throws Exception {
    AtomicLong currentTimeMillis = new AtomicLong(Integer.MAX_VALUE);
    Server server1 = new Server("server1", null, 5, null) {
      @Override
      protected long getCurrentTimeMillis(Clock clock) {
        return currentTimeMillis.get();
      }
    };
    Server server2 = new Server("server2", null, 5, null) {
      @Override
      protected long getCurrentTimeMillis(Clock clock) {
        return currentTimeMillis.get();
      }
    };
    var servers = List.of(server1, server2);
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    int slowStartInterval = 2;
    ApplicationConfig applicationConfig = buildTestConfig();
    applicationConfig.getHosts().get(DEFAULT).getProfiles().get(DEFAULT).setSlowStartIntervalSec(3);
    when(configStore.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));

    createHttpClientFactory(List.of(TEST_UPSTREAM));
    var calledAddresses = new ArrayList<>();
    when(httpClient.executeRequest(any(Request.class), any(CompletionHandler.class))).then(iom -> {
      Request request = completeWith(200, iom);
      calledAddresses.add(request.getUri().getHost());
      return null;
    });

    getTestClient().get();
    getTestClient().get();
    getTestClient().get();
    assertEquals(3, server1.getStatsRequests());
    assertEquals(0, server2.getStatsRequests());
    currentTimeMillis.addAndGet(TimeUnit.SECONDS.toMillis(slowStartInterval + 1));
    getTestClient().get();
    assertEquals(4, server1.getStatsRequests());
    assertEquals(server1.getAddress(), calledAddresses.get(calledAddresses.size() - 1));
    assertEquals(3, server2.getStatsRequests());
    getTestClient().get();
    assertEquals(4, server2.getStatsRequests());
    assertEquals(server2.getAddress(), calledAddresses.get(calledAddresses.size() - 1));
  }

  @Test
  public void testBalancingWithCrossDC() throws Exception {
    Server server1 = new Server("server1", null, 10, "anotherDC");
    Server server2 = new Server("server1", null, 5, null);
    Server server3 = new Server("server1", null, 1, null);
    List<Server> servers = List.of(
        server1,
        server2,
        server3
    );
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    ApplicationConfig applicationConfig = buildTestConfig();

    when(configStore.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));

    createHttpClientFactory(List.of(TEST_UPSTREAM));

    int size = 5;

    when(httpClient.executeRequest(any(Request.class), any(CompletionHandler.class)))
        .then(iom -> {
          completeWith(200, iom);
          return null;
        });

    for (int i = 0; i < size; i++) {
      getTestClient().get();
    }

    assertEquals(0, servers.get(0).getCurrentRequests());
    assertEquals(0, servers.get(0).getStatsRequests());

    assertEquals(0, servers.get(1).getCurrentRequests());
    assertEquals(4, servers.get(1).getStatsRequests());

    assertEquals(0, servers.get(2).getCurrentRequests());
    assertEquals(1, servers.get(2).getStatsRequests());
  }

  @Test
  public void requestWithProfile() throws Exception {
    String profileFoo = "foo";
    String profileBar = "bar";
    Map<String, Profile> profiles = Map.of(
        DEFAULT, new Profile().setRequestTimeoutSec(33f),
        profileFoo, new Profile().setRequestTimeoutSec(22f),
        profileBar, new Profile().setRequestTimeoutSec(11f)
    );

    ApplicationConfig applicationConfig = new ApplicationConfig().setHosts(Map.of(DEFAULT, new Host().setProfiles(profiles)));

    when(configStore.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));

    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(List.of(new Server("server1", null, 1, null)));

    List<String> upstreamList = List.of(TEST_UPSTREAM);
    createHttpClientFactory(upstreamList);
    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class))).then(iom -> {
      request[0] = completeWith(200, iom);
      return null;
    });

    getTestClient().get();
    assertRequestTimeoutEquals(request[0], TimeUnit.SECONDS.toMillis(33));
    getTestClient().withPreconfiguredEngine(RequestBalancerBuilder.class, builder -> builder.withProfile("foo")).get();
    assertRequestTimeoutEquals(request[0], TimeUnit.SECONDS.toMillis(22));
    getTestClient().withPreconfiguredEngine(RequestBalancerBuilder.class, builder -> builder.withProfile("foo")).getWithProfileInsideClient("bar");
    assertRequestTimeoutEquals(request[0], TimeUnit.SECONDS.toMillis(11));
  }

  @Test
  public void requestWithProfileMissing() {
    createHttpClientFactory();

    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });
    assertThrows(
        IllegalStateException.class,
        () -> getTestClient().withPreconfiguredEngine(RequestBalancerBuilder.class, builder -> builder.withProfile("not existing profile")).get()
    );
  }

  @Test()
  public void shouldGetDefaultProfileInsteadEmpty() throws Exception {
    createHttpClientFactory();

    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });
    getTestClient().withPreconfiguredEngine(RequestBalancerBuilder.class, builder -> builder.withProfile(null)).get();
    assertRequestTimeoutEquals(request[0], TimeUnit.SECONDS.toMillis(2));
  }

  @Test
  public void preconfiguredWithWrongClass() {

    createHttpClientFactory();

    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });
    assertThrows(
        ClassCastException.class,
        () -> getTestClient().withPreconfiguredEngine(NotValidEngineBuilder.class, NotValidEngineBuilder::withSmth).get()
    );
  }

  @Test
  public void configuredWithWrongClass() {
    createHttpClientFactory();

    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });
    assertThrows(ClassCastException.class, () -> getTestClient().getWrongEngineBuilderClass());
  }

  @Test
  public void requestWithUnknownUpstream() throws Exception {

    List<String> upstreamList = List.of(TEST_UPSTREAM);

    createHttpClientFactory(upstreamList);

    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });
    withContext(Map.of(HttpHeaderNames.X_OUTER_TIMEOUT_MS, List.of(Long.toString(TimeUnit.SECONDS.toMillis(1)))));
    getTestClient().get("https://foo/get");
    assertHostEquals(request[0], "foo");
  }

  @Test
  public void requestAndUpdateServers() throws Exception {
    when(serverStore.getServers(TEST_UPSTREAM))
        .thenReturn(List.of(new Server("server1", null, 1, null)));
    createHttpClientFactory();

    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });

    getTestClient().get();
    assertHostEquals(request[0], "server1");
    when(serverStore.getServers(TEST_UPSTREAM))
        .thenReturn(List.of(new Server("server2", null, 1, null)));
    upstreamManager.updateUpstreams(Set.of(TEST_UPSTREAM));

    getTestClient().get();
    assertHostEquals(request[0], "server2");

    when(serverStore.getServers(TEST_UPSTREAM))
        .thenReturn(List.of(new Server("server2", null, 1, null), new Server("server3", null, 1, null)));
    upstreamManager.updateUpstreams(Set.of(TEST_UPSTREAM));

    getTestClient().get();
    assertHostEquals(request[0], "server2");

    getTestClient().get();
    assertHostEquals(request[0], "server3");
  }

  @Test
  public void shouldNotRetryRequestTimeoutForPost() {
    createHttpClientFactory();

    Request[] request = new Request[1];

    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = failWith(new TimeoutException("Request timed out"), iom);
          return null;
        });

    try {
      getTestClient().post();
    } catch (Exception e) {
      assertRequestEquals(request, "server1");
      testEventListener.assertCalled(REQUEST, RESPONSE, FINISHED);
    }
  }

  @Test
  public void disallowCrossDCRequests() throws Exception {
    List<Server> servers = List.of(new Server("server1", null, 1, "DC1"), new Server("server2", null, 1, "DC2"));
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    createHttpClientFactory(List.of(TEST_UPSTREAM), "DC1", false);

    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });

    getTestClient().get();
    assertHostEquals(request[0], "server1");

    getTestClient().get();
    assertHostEquals(request[0], "server1");

    getTestClient().get();
    assertHostEquals(request[0], "server1");
  }

  @Test
  public void balancedRequestMonitoring() throws Exception {
    String datacenter = "DC1";
    createHttpClientFactory(List.of(TEST_UPSTREAM), datacenter, false);
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(List.of(new Server("server1", null, 1, datacenter)));
    upstreamManager.updateUpstreams(Set.of(TEST_UPSTREAM));

    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          completeWith(200, iom);
          return null;
        });

    http
        .with(new RequestBuilder("GET").setUrl("http://backend/path?query").build())
        .expectPlainText()
        .result()
        .get();

    Monitoring monitoring = upstreamManager.getMonitoring().stream().findFirst().get();
    verify(monitoring).countRequest(
        eq("backend"),
        eq(datacenter),
        eq("server1"),
        any(),
        eq(200),
        anyLong(),
        eq(true),
        eq(getBalancingStrategyTypeForUpstream().getPublicName())
    );
  }

  @Test
  public void unbalancedRequestMonitoring() throws Exception {
    createHttpClientFactory();

    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          completeWith(200, iom);
          return null;
        });

    http
        .with(new RequestBuilder("GET").setUrl("https://not-balanced-backend/path?query").build())
        .expectPlainText()
        .result()
        .get();

    Monitoring monitoring = upstreamManager.getMonitoring().stream().findFirst().get();
    verify(monitoring).countRequest(
        eq("https://not-balanced-backend"),
        eq(ExternalUrlRequestor.DC_FOR_EXTERNAL_REQUESTS),
        eq("https://not-balanced-backend"),
        any(),
        eq(200),
        anyLong(),
        eq(true),
        eq("externalRequest")
    );
  }

  @Test
  public void failIfNoBackendAvailableInCurrentDC() {
    createHttpClientFactory(List.of(TEST_UPSTREAM), "DC1", false);
    when(serverStore.getServers(TEST_UPSTREAM))
        .thenReturn(List.of(new Server("server1", null, 1, "DC2")));

    assertThrows(ExecutionException.class, () -> getTestClient().get());
  }

  @Test
  public void testAllowCrossDCRequests() throws Exception {
    List<Server> servers = List.of(new Server("server1", null, 1, "DC1"), new Server("server2", null, 1, "DC2"));
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    createHttpClientFactory(List.of(TEST_UPSTREAM), "DC1", true);

    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });

    getTestClient().get();
    assertHostEquals(request[0], "server1");

    getTestClient().get();
    assertHostEquals(request[0], "server1");

    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(List.of(new Server("server2", null, 1, "DC2")));
    upstreamManager.updateUpstreams(Set.of(TEST_UPSTREAM));

    getTestClient().get();
    assertHostEquals(request[0], "server2");
  }

  @Test
  public void testTimeoutMultiplierWithCustomizedCopy() throws Exception {
    double defaultMultiplier = 3.5;
    createHttpClientFactory(defaultMultiplier);

    int defaultTimeout = UpstreamConfig.DEFAULT_CONFIG.getRequestTimeoutMs();

    Request[] request = new Request[1];
    when(httpClient.executeRequest(any(Request.class), any(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });

    // identical copy
    getTestClient().withPreconfiguredEngine(RequestBalancerBuilder.class, UnaryOperator.identity()).get();
    assertRequestTimeoutEquals(request[0], defaultTimeout * defaultMultiplier);

    // different multiplier
    getTestClient().withPreconfiguredEngine(RequestBalancerBuilder.class, builder -> builder.withTimeoutMultiplier(2.5)).get();
    assertRequestTimeoutEquals(request[0], defaultTimeout * 2.5);

    // different multiplier and then identity
    getTestClient()
        .withPreconfiguredEngine(RequestBalancerBuilder.class, builder -> builder.withTimeoutMultiplier(1.5))
        .withPreconfiguredEngine(RequestBalancerBuilder.class, UnaryOperator.identity())
        .get();
    assertRequestTimeoutEquals(request[0], defaultTimeout * 1.5);

    // change something else
    getTestClient().withPreconfiguredEngine(RequestBalancerBuilder.class, RequestBalancerBuilder::forceIdempotence).get();
    assertRequestTimeoutEquals(request[0], defaultTimeout * defaultMultiplier);
  }

  @Test
  public void testHostsWithSession() throws Exception {
    AtomicLong currentTimeMillis = new AtomicLong(Integer.MAX_VALUE);
    Server server1 = new Server("server1", null, 5, null) {
      @Override
      protected long getCurrentTimeMillis(Clock clock) {
        return currentTimeMillis.get();
      }
    };

    var servers = List.of(server1);
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    ApplicationConfig applicationConfig = buildTestConfig();
    applicationConfig.getHosts().get(DEFAULT).getProfiles().get(DEFAULT).setSessionRequired(true);
    when(configStore.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));

    createHttpClientFactory(List.of(TEST_UPSTREAM));
    Request[] request = new Request[1];
    when(httpClient.executeRequest(any(Request.class), any(CompletionHandler.class))).then(iom -> {
      request[0] = completeWith(200, iom);
      return null;
    });

    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HttpHeaderNames.HH_PROTO_SESSION, singletonList("somesession"));
    withContext(headers);
    getTestClient().get(TEST_UPSTREAM);

    assertEquals("somesession", request[0].getHeaders().get(HttpHeaderNames.HH_PROTO_SESSION));
  }

  @Test
  public void testHostsWithoutSession() throws Exception {
    AtomicLong currentTimeMillis = new AtomicLong(Integer.MAX_VALUE);
    Server server1 = new Server("server1", null, 5, null) {
      @Override
      protected long getCurrentTimeMillis(Clock clock) {
        return currentTimeMillis.get();
      }
    };

    var servers = List.of(server1);
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    ApplicationConfig applicationConfig = buildTestConfig();
    applicationConfig.getHosts().get(DEFAULT).getProfiles().get(DEFAULT).setSessionRequired(false);
    when(configStore.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));

    createHttpClientFactory(List.of(TEST_UPSTREAM));
    Request[] request = new Request[1];
    when(httpClient.executeRequest(any(Request.class), any(CompletionHandler.class))).then(iom -> {
      request[0] = completeWith(200, iom);
      return null;
    });

    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HttpHeaderNames.HH_PROTO_SESSION, singletonList("somesession"));
    withContext(headers);
    getTestClient().get(TEST_UPSTREAM);

    assertNull(request[0].getHeaders().get(HttpHeaderNames.HH_PROTO_SESSION));
  }

  @Override
  protected boolean isAdaptiveClient() {
    return false;
  }

  @Override
  protected BalancingStrategyType getBalancingStrategyTypeForUpstream() {
    return BalancingStrategyType.WEIGHTED;
  }
}
