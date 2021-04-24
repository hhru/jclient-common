package ru.hh.jclient.common;

import joptsimple.internal.Strings;
import org.asynchttpclient.Request;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import ru.hh.jclient.common.HttpClientImpl.CompletionHandler;
import static ru.hh.jclient.common.TestRequestDebug.Call.FINISHED;
import static ru.hh.jclient.common.TestRequestDebug.Call.REQUEST;
import static ru.hh.jclient.common.TestRequestDebug.Call.RESPONSE;
import ru.hh.jclient.common.balancing.ExternalUrlRequestor;
import ru.hh.jclient.common.balancing.RequestBalancerBuilder;
import ru.hh.jclient.common.balancing.Server;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT;
import static ru.hh.jclient.common.balancing.UpstreamConfigParserTest.buildTestConfig;
import ru.hh.jclient.consul.model.ApplicationConfig;
import ru.hh.jclient.consul.model.Host;
import ru.hh.jclient.consul.model.Profile;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public class BalancingClientTest extends BalancingClientTestBase {
  private static final String PROFILE_DELIMITER = ":";

  @Test
  public void testBalancing() throws Exception {
    Server server1 = new Server("server1", 10, null);
    Server server2 = new Server("server1", 5, null);
    Server server3 = new Server("server1", 1, null);
    List<Server> servers = List.of(server1,
        server2,
        server3
    );
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    ApplicationConfig applicationConfig = buildTestConfig();

    when(upstreamConfigService.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(applicationConfig);

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

    assertEquals(0, servers.get(0).getRequests());
    assertEquals(6, servers.get(0).getStatsRequests());

    assertEquals(0, servers.get(1).getRequests());
    assertEquals(3, servers.get(1).getStatsRequests());

    assertEquals(0, servers.get(2).getRequests());
    assertEquals(1, servers.get(2).getStatsRequests());
  }

  @Test
  public void testAddServer() throws Exception {
    Server existingServer = new Server("server1", 3, null);
    List<Server> servers = new ArrayList<>();
    servers.add(existingServer);
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    ApplicationConfig applicationConfig = buildTestConfig();

    when(upstreamConfigService.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(applicationConfig);

    createHttpClientFactory(List.of(TEST_UPSTREAM));
    var calledAddresses = new ArrayList<>();
    when(httpClient.executeRequest(any(Request.class), any(CompletionHandler.class))).then(iom -> {
      Request request = completeWith(200, iom);
      calledAddresses.add(request.getUri().getHost());
      return null;
    });
    // less calls than weight, otherwise statRequests is rescaled
    getTestClient().get();
    Server newServer = new Server("server2", 3, null);
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
    Server server1 = new Server("server1", 3, null);
    Server server2 = new Server("server2", 3, null);
    List<Server> servers = List.of(server1, server2);
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    ApplicationConfig applicationConfig = buildTestConfig();

    when(upstreamConfigService.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(applicationConfig);

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
    Server server1 = new Server("server1", 5, null) {
      @Override
      protected long getCurrentTimeMillis(Clock clock) {
        return currentTimeMillis.get();
      }
    };
    Server server2 = new Server("server2", 5, null) {
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
    when(upstreamConfigService.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(applicationConfig);

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
    Server server1 = new Server("server1", 10, "anotherDC");
    Server server2 = new Server("server1", 5, null);
    Server server3 = new Server("server1", 1, null);
    List<Server> servers = List.of(server1,
        server2,
        server3
    );
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    ApplicationConfig applicationConfig = buildTestConfig();

    when(upstreamConfigService.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(applicationConfig);

    createHttpClientFactory(List.of(TEST_UPSTREAM));

    int size=5;

    when(httpClient.executeRequest(any(Request.class), any(CompletionHandler.class)))
        .then(iom -> {
          completeWith(200, iom);
          return null;
        });

    for (int i = 0; i < size; i++) {
      getTestClient().get();
    }

    assertEquals(0, servers.get(0).getRequests());
    assertEquals(0, servers.get(0).getStatsRequests());

    assertEquals(0, servers.get(1).getRequests());
    assertEquals(4, servers.get(1).getStatsRequests());

    assertEquals(0, servers.get(2).getRequests());
    assertEquals(1, servers.get(2).getStatsRequests());
  }

  @Test
  public void requestWithProfile() throws Exception {
    String profileFoo = "foo";
    String profileBar = "bar";
    Map<String, Profile> profiles = Map.of(
        DEFAULT, new Profile().setRequestTimeoutMs(33f),
        profileFoo, new Profile().setRequestTimeoutMs(22f),
        profileBar, new Profile().setRequestTimeoutMs(11f)
    );

    ApplicationConfig applicationConfig = new ApplicationConfig().setHosts(Map.of(DEFAULT, new Host().setProfiles(profiles)));

    when(upstreamConfigService.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(applicationConfig);

    when(serverStore.getServers(TEST_UPSTREAM + ":" + profileFoo)).thenReturn(List.of(new Server("server1", 1, null)));
    when(serverStore.getServers(TEST_UPSTREAM + ":" + profileBar)).thenReturn(List.of(new Server("server1", 1, null)));

    List<String> upstreamList = List.of(TEST_UPSTREAM, profileName(TEST_UPSTREAM, profileFoo), profileName(TEST_UPSTREAM, profileBar));
    createHttpClientFactory(upstreamList);
    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
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

  @Test(expected = IllegalStateException.class)
  public void requestWithProfileMissing() throws Exception {
    createHttpClientFactory();

    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });
    getTestClient().withPreconfiguredEngine(RequestBalancerBuilder.class, builder -> builder.withProfile("not existing profile")).get();
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

  @Test(expected = ClassCastException.class)
  public void preconfiguredWithWrongClass() throws Exception {

    createHttpClientFactory();

    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
            .then(iom -> {
              request[0] = completeWith(200, iom);
              return null;
            });
    getTestClient().withPreconfiguredEngine(NotValidEngineBuilder.class, NotValidEngineBuilder::withSmth).get();
    assertRequestTimeoutEquals(request[0], TimeUnit.SECONDS.toMillis(2));
  }

  @Test(expected = ClassCastException.class)
  public void configuredWithWrongClass() throws Exception {
    createHttpClientFactory();

    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
            .then(iom -> {
              request[0] = completeWith(200, iom);
              return null;
            });
    getTestClient().getWrongEngineBuilderClass();
    assertRequestTimeoutEquals(request[0], TimeUnit.SECONDS.toMillis(2));
  }

  @Test
  public void requestWithUnknownUpstream() throws Exception {

    List<String> upstreamList = List.of(profileName(TEST_UPSTREAM, "foo"), profileName(TEST_UPSTREAM, "bar"));

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
            .thenReturn(List.of(new Server("server1", 1, null)));
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
            .thenReturn(List.of(new Server("server2", 1, null)));
    upstreamManager.updateUpstreams(Set.of(TEST_UPSTREAM), false);

    getTestClient().get();
    assertHostEquals(request[0], "server2");

    when(serverStore.getServers(TEST_UPSTREAM))
            .thenReturn(List.of(new Server("server2", 1, null), new Server("server3", 1, null)));
    upstreamManager.updateUpstreams(Set.of(TEST_UPSTREAM), false);

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
      debug.assertCalled(REQUEST, RESPONSE, FINISHED);
    }
  }

  @Test
  public void disallowCrossDCRequests() throws Exception {
    List<Server> servers = List.of(new Server("server1", 1, "DC1"), new Server("server2", 1, "DC2"));
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
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(List.of(new Server("server1", 1, datacenter)));
    upstreamManager.updateUpstreams(Set.of(TEST_UPSTREAM), true);

    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
      .then(iom -> {
        completeWith(200, iom);
        return null;
      });

    http.with(new RequestBuilder("GET").setUrl("http://backend/path?query").build())
      .expectPlainText().result().get();

    Monitoring monitoring = upstreamManager.getMonitoring().stream().findFirst().get();
    verify(monitoring).countRequest(
      eq("backend"), eq(datacenter), eq("server1"), eq(200), anyLong(), eq(true)
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

    http.with(new RequestBuilder("GET").setUrl("https://not-balanced-backend/path?query").build())
      .expectPlainText().result().get();

    Monitoring monitoring = upstreamManager.getMonitoring().stream().findFirst().get();
    verify(monitoring).countRequest(
      eq("https://not-balanced-backend"), eq(ExternalUrlRequestor.DC_FOR_EXTERNAL_REQUESTS),
      eq("https://not-balanced-backend"),
      eq(200), anyLong(), eq(true)
    );
  }

  @Test(expected = ExecutionException.class)
  public void failIfNoBackendAvailableInCurrentDC() throws Exception {
    createHttpClientFactory(List.of(TEST_UPSTREAM), "DC1", false);
    when(serverStore.getServers(TEST_UPSTREAM))
            .thenReturn(List.of(new Server("server1", 1, "DC2")));

    getTestClient().get();
  }

  @Test
  public void testAllowCrossDCRequests() throws Exception {
    List<Server> servers = List.of(new Server("server1", 1, "DC1"), new Server("server2", 1, "DC2"));
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

    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(List.of(new Server("server2", 1, "DC2")));
    upstreamManager.updateUpstreams(Set.of(TEST_UPSTREAM), false);

    getTestClient().get();
    assertHostEquals(request[0], "server2");
  }

  @Override
  public boolean isAdaptive() {
    return false;
  }


  private String profileName(String serviceName, String profileName) {
    String[] ar = {serviceName, profileName};
    return Strings.join(ar, PROFILE_DELIMITER);
  }

}
