package ru.hh.jclient.common;

import io.netty.channel.ConnectTimeoutException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import ru.hh.jclient.common.HttpClientImpl.CompletionHandler;
import static ru.hh.jclient.common.HttpHeaderNames.CONTENT_TYPE;
import static ru.hh.jclient.common.TestRequestDebug.Call.FINISHED;
import static ru.hh.jclient.common.TestRequestDebug.Call.REQUEST;
import static ru.hh.jclient.common.TestRequestDebug.Call.RESPONSE;
import static ru.hh.jclient.common.TestRequestDebug.Call.RESPONSE_CONVERTED;
import static ru.hh.jclient.common.TestRequestDebug.Call.RETRY;
import ru.hh.jclient.common.balancing.BalancingRequestStrategy;
import ru.hh.jclient.common.balancing.BalancingUpstreamManager;
import ru.hh.jclient.common.balancing.ConfigStore;
import ru.hh.jclient.common.balancing.JClientInfrastructureConfig;
import ru.hh.jclient.common.balancing.RequestBalancerBuilder;
import ru.hh.jclient.common.balancing.Server;
import ru.hh.jclient.common.balancing.ServerStore;
import ru.hh.jclient.common.balancing.UpstreamConfig;
import static ru.hh.jclient.common.balancing.UpstreamConfig.DEFAULT;
import ru.hh.jclient.common.balancing.config.ApplicationConfig;
import static ru.hh.jclient.common.balancing.config.ApplicationConfigTest.buildTestConfig;
import ru.hh.jclient.common.balancing.config.RetryPolicyConfig;
import ru.hh.jclient.common.exception.ClientResponseException;
import static ru.hh.jclient.common.util.ContentType.TEXT_PLAIN_UTF_8;
import ru.hh.jclient.common.util.storage.SingletonStorage;
import ru.hh.jclient.consul.TestUpstreamConfigService;
import ru.hh.jclient.consul.TestUpstreamService;

abstract class BalancingClientTestBase extends HttpClientTestBase {

  static final String TEST_UPSTREAM = "backend";
  AsyncHttpClient httpClient;
  BalancingRequestStrategy requestingStrategy;
  BalancingUpstreamManager upstreamManager;
  ConfigStore configStore = mock(ConfigStore.class);
  ServerStore serverStore = mock(ServerStore.class);

  @Before
  public void setUpTest() {
    withEmptyContext();
    httpClient = mock(AsyncHttpClient.class);
    when(httpClient.getConfig()).thenReturn(httpClientConfig);
    when(configStore.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(ApplicationConfig.toUpstreamConfigs(new ApplicationConfig(), DEFAULT));
    when(serverStore.getServers(TEST_UPSTREAM))
        .thenReturn(List.of(new Server("server1", 1, null), new Server("server2", 2, null)));
  }

  @Test
  public void shouldMakeGetRequestForSingleServer() throws Exception {
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(List.of(new Server("server1", 1, null)));

    createHttpClientFactory(List.of(TEST_UPSTREAM));

    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });

    getTestClient().get();

    assertHostEquals(request[0], "server1");

    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test(expected = ExecutionException.class)
  public void retryShouldFailIfNoServersAvailable() throws Exception {
    createHttpClientFactory(List.of(TEST_UPSTREAM));

    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          failWith(new TimeoutException("Request timed out 1"), iom);
          return null;
        })
        .then(iom -> {
          failWith(new TimeoutException("Request timed out 2"), iom);
          return null;
        });

    getTestClient().get();
  }

  @Test
  public void retryIOExceptionRemotelyClosed() throws Exception {
    createHttpClientFactory(List.of(TEST_UPSTREAM));

    Request[] request = mockRetryIOException("Remotely closed");
    getTestClient().get();

    assertRequestEquals(request, "server1", "server2");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retryIdempotentIOExceptionResetByPeer() throws Exception {
    createHttpClientFactory(List.of(TEST_UPSTREAM));

    Request[] request = mockRetryIOException("Connection reset by peer");
    getTestClient().get();

    assertRequestEquals(request, "server1", "server2");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void doNotRetryNonIdempotentIOExceptionResetByPeer() throws Exception {
    createHttpClientFactory(List.of(TEST_UPSTREAM));

    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          failWith(new IOException("Connection reset by peer"), iom);
          return null;
        });

    try {
      getTestClient().post();
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof ClientResponseException);
      assertEquals(599, ((ClientResponseException) e.getCause()).getStatusCode());

      debug.assertCalled(REQUEST, RESPONSE, FINISHED);
    }
  }

  private Request[] mockRetryIOException(String exceptionText) {
    Request[] request = new Request[2];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = failWith(new IOException(exceptionText), iom);
          return null;
        })
        .then(iom -> {
          request[1] = completeWith(200, iom);
          return null;
        });
    return request;
  }

  @Test
  public void retryConnectException() throws Exception {
    List<Server> servers = List.of(
        new Server("server1", 1, null),
        new Server("server2", 1, null),
        new Server("server3", 1, null),
        new Server("server4", 1, null)
    );
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    ApplicationConfig applicationConfig = buildTestConfig();
    applicationConfig.getHosts().get(DEFAULT).getProfiles().get(DEFAULT).setMaxTries(4);

    when(configStore.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));

    createHttpClientFactory(List.of(TEST_UPSTREAM));

    Request[] request = new Request[4];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = failWith(new ConnectException("Connection refused"), iom);
          return null;
        })
        .then(iom -> {
          request[1] = failWith(new ConnectException("Connection reset by peer"), iom);
          return null;
        })
        .then(iom -> {
          request[2] = failWith(new ConnectException("No route to host"), iom);
          return null;
        })
        .then(iom -> {
          request[3] = completeWith(200, iom);
          return null;
        });

    getTestClient().get();

    assertRequestEquals(request, "server1", "server2", "server3", "server4");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RETRY, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retry503() throws Exception {
    List<Server> servers = List.of(
        new Server("server1", 1, null),
        new Server("server2", 1, null),
        new Server("server3", 1, null)
    );
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    ApplicationConfig applicationConfig = buildTestConfig();
    applicationConfig.getHosts().get(DEFAULT).getProfiles().get(DEFAULT).setMaxTries(3);

    when(configStore.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));

    createHttpClientFactory(List.of(TEST_UPSTREAM));

    Request[] request = mockRequestWith503Response();

    getTestClient().get();

    assertRequestEquals(request, "server1", "server2", "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retryTimeoutException() throws Exception {
    createHttpClientFactory();

    Request[] request = new Request[2];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = failWith(new TimeoutException("Connect timed out"), iom);
          return null;
        })
        .then(iom -> {
          request[1] = completeWith(200, iom);
          return null;
        });

    getTestClient().get();

    assertRequestEquals(request, "server1", "server2");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }


  //test set multiplier HttpClientFactoryBuilderTest.testMultiplierApplication()
  @Test
  public void testRequestTimeoutForUpstream() throws Exception {
    double multiplier = 3.5;
    createHttpClientFactory(multiplier);

    Request[] request = new Request[1];
    when(httpClient.executeRequest(any(Request.class), any(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });

    int customTimeout = 123;
    //with timeout from request
    getTestClient().getWithTimeout(customTimeout);
    assertEquals((int) (customTimeout * multiplier), request[0].getRequestTimeout());

    //with timeout from profile
    getTestClient().get();
    assertEquals((int) (UpstreamConfig.DEFAULT_CONFIG.getRequestTimeoutMs() * multiplier), request[0].getRequestTimeout());
  }

  @Test
  public void testRequestTimeoutForExternalUrl() throws Exception {
    double multiplier = 3.5;
    createHttpClientFactory(multiplier);

    Request[] request = new Request[1];
    when(httpClient.executeRequest(any(Request.class), any(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });

    int customTimeout = 123;
    //with timeout from request
    getTestClient("external_url").getWithTimeout(customTimeout);
    assertEquals((int) (customTimeout * multiplier), request[0].getRequestTimeout());
  }

  @Test
  public void retry503ForNonIdempotentRequest() throws Exception {
    List<Server> servers = List.of(
        new Server("server1", 1, null),
        new Server("server2", 1, null),
        new Server("server3", 1, null)
    );
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);
    ApplicationConfig applicationConfig = buildTestConfig();
    applicationConfig
        .getHosts()
        .get(DEFAULT)
        .getProfiles()
        .get(DEFAULT)
        .setMaxTries(3)
        .setRetryPolicy(Map.of(503, new RetryPolicyConfig().setIdempotent(true)));

    when(configStore.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));

    createHttpClientFactory();
    Request[] request = mockRequestWith503Response();

    getTestClient().post();

    assertRequestEquals(request, "server1", "server2", "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retryConnectTimeoutException() throws Exception {
    List<Server> servers = List.of(
        new Server("server1", 1, null),
        new Server("server2", 1, null),
        new Server("server3", 1, null)
    );
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    ApplicationConfig applicationConfig = buildTestConfig();
    applicationConfig.getHosts().get(DEFAULT).getProfiles().get(DEFAULT).setMaxTries(3);

    when(configStore.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));

    createHttpClientFactory();

    Request[] request = mockRequestWithConnectTimeoutResponse();

    getTestClient().get();

    assertRequestEquals(request, "server1", "server2", "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retryConnectTimeoutExceptionForNonIdempotentRequest() throws Exception {
    List<Server> servers = List.of(
        new Server("server1", 1, null),
        new Server("server2", 1, null),
        new Server("server3", 1, null)
    );
    when(serverStore.getServers(TEST_UPSTREAM)).thenReturn(servers);

    ApplicationConfig applicationConfig = buildTestConfig();
    applicationConfig.getHosts().get(DEFAULT).getProfiles().get(DEFAULT).setMaxTries(3);

    when(configStore.getUpstreamConfig(TEST_UPSTREAM)).thenReturn(ApplicationConfig.toUpstreamConfigs(applicationConfig, DEFAULT));

    createHttpClientFactory();
    Request[] request = mockRequestWithConnectTimeoutResponse();

    getTestClient().post();

    assertRequestEquals(request, "server1", "server2", "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }


  void createHttpClientFactory(List<String> upstreamList, String datacenter, boolean allowCrossDCRequests) {
    http = createHttpClientFactory(httpClient, datacenter, upstreamList, allowCrossDCRequests, HttpClientFactoryBuilder.DEFAULT_TIMEOUT_MULTIPLIER);
  }

  void createHttpClientFactory(double multiplier) {
    http = createHttpClientFactory(httpClient, null, List.of(TEST_UPSTREAM), false, multiplier);
  }

  void createHttpClientFactory() {
    http = createHttpClientFactory(httpClient, null, List.of(TEST_UPSTREAM), false, HttpClientFactoryBuilder.DEFAULT_TIMEOUT_MULTIPLIER);
  }

  void createHttpClientFactory(List<String> upstreamList) {
    http = createHttpClientFactory(httpClient, null, upstreamList, true, HttpClientFactoryBuilder.DEFAULT_TIMEOUT_MULTIPLIER);
  }

  Request completeWith(int status, InvocationOnMock iom) throws Exception {
    Response response = mock(Response.class);

    when(response.getStatusCode()).thenReturn(status);
    when(response.getHeader(eq(CONTENT_TYPE))).thenReturn(TEXT_PLAIN_UTF_8);
    when(response.getUri()).thenReturn(mock(org.asynchttpclient.uri.Uri.class));
    Request request = iom.getArgument(0);
    CompletionHandler handler = iom.getArgument(1);
    handler.onCompleted(response);
    return request;
  }

  static void assertHostEquals(Request request, String host) {
    assertEquals(host, request.getUri().getHost());
  }

  static void assertRequestTimeoutEquals(Request request, long timeoutMs) {
    assertEquals((int) timeoutMs, request.getRequestTimeout());
  }

  static void assertRequestTimeoutEquals(Request request, double timeoutMs) {
    assertRequestTimeoutEquals(request, (long) timeoutMs);
  }

  private HttpClientFactory createHttpClientFactory(
      AsyncHttpClient httpClient,
      String datacenter,
      List<String> upstreamList,
      boolean allowCrossDCRequests,
      double multiplier
  ) {
    Monitoring monitoring = mock(Monitoring.class);
    JClientInfrastructureConfig infrastructureConfig = mock(JClientInfrastructureConfig.class);
    when(infrastructureConfig.getCurrentDC()).thenReturn(datacenter);
    upstreamManager = new BalancingUpstreamManager(
        configStore,
        serverStore,
        Set.of(monitoring),
        infrastructureConfig,
        allowCrossDCRequests
    );
    upstreamManager.updateUpstreams(upstreamList);
    requestingStrategy = new BalancingRequestStrategy(upstreamManager, new TestUpstreamService(), new TestUpstreamConfigService())
        .createCustomizedCopy(requestBalancerBuilder -> requestBalancerBuilder.withTimeoutMultiplier(multiplier));
    return new HttpClientFactory(httpClient, new SingletonStorage<>(() -> httpClientContext), Set.of(), Runnable::run, requestingStrategy);
  }

  Request failWith(Throwable t, InvocationOnMock iom) {
    Request request = iom.getArgument(0);
    CompletionHandler handler = iom.getArgument(1);
    handler.onThrowable(t);
    return request;
  }

  protected abstract boolean isAdaptive();

  void assertRequestEquals(Request[] request, String... actual) {
    if (isAdaptive()) {
      assertTrue(toSet(request).containsAll(toSet(actual)));
      assertTrue(toSet(actual).containsAll(toSet(request)));
    } else {
      assertEquals(request.length, actual.length);
      for (int i = 0; i < request.length; i++) {
        assertHostEquals(request[i], actual[i]);
      }
    }
  }

  private Request[] mockRequestWith503Response() {
    Request[] request = new Request[3];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(503, iom);
          return null;
        })
        .then(iom -> {
          request[1] = completeWith(503, iom);
          return null;
        })
        .then(iom -> {
          request[2] = completeWith(200, iom);
          return null;
        });
    return request;
  }

  private Request[] mockRequestWithConnectTimeoutResponse() {
    Request[] request = new Request[3];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = failWith(new ConnectTimeoutException("Connect timed out"), iom);
          return null;
        })
        .then(iom -> {
          request[1] = failWith(new ConnectTimeoutException("Connect timed out"), iom);
          return null;
        })
        .then(iom -> {
          request[2] = completeWith(200, iom);
          return null;
        });
    return request;
  }

  private static <T> Set<T> toSet(T[] array) {
    return new HashSet<>(Arrays.asList(array));
  }

  private static Set<String> toSet(Request[] requests) {
    return Arrays.stream(requests).map(r -> r.getUri().getHost()).collect(Collectors.toSet());
  }

  TestClient getTestClient() {
    return getTestClient(TEST_UPSTREAM);
  }

  TestClient getTestClient(String upstream) {
    return new TestClient(http, isAdaptive(), upstream);
  }

  static class TestClient extends ConfigurableJClientBase<TestClient> {
    private final boolean adaptive;

    TestClient(HttpClientFactory http, boolean adaptive, String upstream) {
      super("http://" + upstream, http);
      this.adaptive = adaptive;
    }

    void getWithTimeout(int requestTimeout) throws Exception {
      ru.hh.jclient.common.Request request = super.get(url("/get")).setRequestTimeout(requestTimeout).build();
      HttpClient client = getHttp().with(request);
      if (adaptive) {
        client = client.configureRequestEngine(RequestBalancerBuilder.class).makeAdaptive().backToClient();
      }
      client.expectPlainText().result().get();
    }

    void get() throws Exception {
      getWithTimeout(0);
    }

    void post() throws Exception {
      ru.hh.jclient.common.Request request = post(url("/post")).build();
      HttpClient client = getHttp().with(request);
      if (adaptive) {
        client = client.configureRequestEngine(RequestBalancerBuilder.class).makeAdaptive().backToClient();
      }
      client.expectPlainText().result().get();
    }

    void get(String url) throws Exception {
      ru.hh.jclient.common.Request request = super.get(url).build();
      HttpClient client = getHttp().with(request);
      if (adaptive) {
        client = client.configureRequestEngine(RequestBalancerBuilder.class).makeAdaptive().backToClient();
      }
      client.expectPlainText().result().get();
    }

    void getWrongEngineBuilderClass() throws Exception {
      ru.hh.jclient.common.Request request = super.get(url("/get")).build();
      HttpClient client = getHttp().with(request).configureRequestEngine(NotValidEngineBuilder.class).withSmth().backToClient();
      client.expectPlainText().result().get();
    }

    void getWithProfileInsideClient(String profile) throws Exception {
      ru.hh.jclient.common.Request request = super.get(url("/get")).build();
      HttpClient client = getHttp().with(request).configureRequestEngine(RequestBalancerBuilder.class).withProfile(profile).backToClient();
      client.expectPlainText().result().get();
    }

    @Override
    protected TestClient createCustomizedCopy(HttpClientFactoryConfigurator configurator) {
      return new TestClient(configurator.configure(getHttp()), adaptive, TEST_UPSTREAM);
    }
  }

  static final class NotValidEngineBuilder implements RequestEngineBuilder<NotValidEngineBuilder> {

    @Override
    public RequestEngine build(ru.hh.jclient.common.Request request, RequestStrategy.RequestExecutor executor) {
      return null;
    }

    @Override
    public NotValidEngineBuilder withTimeoutMultiplier(Double timeoutMultiplier) {
      return this;
    }

    public NotValidEngineBuilder withSmth() {
      return this;
    }

    @Override
    public HttpClient backToClient() {
      return null;
    }
  }
}
