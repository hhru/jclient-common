package ru.hh.jclient.common;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.netty.channel.ConnectTimeoutException;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import ru.hh.jclient.common.HttpClientImpl.CompletionHandler;
import static ru.hh.jclient.common.TestRequestDebug.Call.FINISHED;
import static ru.hh.jclient.common.TestRequestDebug.Call.REQUEST;
import static ru.hh.jclient.common.TestRequestDebug.Call.RESPONSE;
import static ru.hh.jclient.common.TestRequestDebug.Call.RESPONSE_CONVERTED;
import static ru.hh.jclient.common.TestRequestDebug.Call.RETRY;
import ru.hh.jclient.common.balancing.BalancingUpstreamManager;
import ru.hh.jclient.common.exception.ClientResponseException;
import ru.hh.jclient.common.util.storage.SingletonStorage;

import java.io.IOException;
import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

abstract class BalancingClientTestBase extends HttpClientTestBase {

  static final String TEST_UPSTREAM = "backend";
  AsyncHttpClient httpClient;
  UpstreamManager upstreamManager;

  @Before
  public void setUpTest() {
    withEmptyContext();
    httpClient = mock(AsyncHttpClient.class);
    when(httpClient.getConfig()).thenReturn(httpClientConfig);
    debug.reset();
  }

  @Test
  public void shouldMakeGetRequestForSingleServer() throws Exception {
    createHttpClientFactory("| server=http://server");

    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });

    getTestClient().get();

    assertHostEquals(request[0], "server");

    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test(expected = ExecutionException.class)
  public void retryShouldFailIfNoServersAvailable() throws Exception {
    createHttpClientFactory("max_tries=2 max_fails=1 fail_timeout_sec=0.01 | server=http://server");

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
    createHttpClientFactory("| server=http://server1 | server=http://server2");

    Request[] request = mockRetryIOException("Remotely closed");
    getTestClient().get();

    assertRequestEquals(request, "server1", "server2");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retryIdempotentIOExceptionResetByPeer() throws Exception {
    createHttpClientFactory("| server=http://server1 | server=http://server2");

    Request[] request = mockRetryIOException("Connection reset by peer");
    getTestClient().get();

    assertRequestEquals(request, "server1", "server2");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void doNotRetryNonIdempotentIOExceptionResetByPeer() throws Exception {
    createHttpClientFactory("| server=http://server1 | server=http://server2");

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
    createHttpClientFactory("max_tries=4 max_fails=2 " +
        "| server=http://server1 | server=http://server2 | server=http://server3 | server=http://server4");

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
    createHttpClientFactory("max_tries=3 max_fails=2 | server=http://server1 | server=http://server2 | server=http://server3");

    Request[] request = mockRequestWith503Response();

    getTestClient().get();

    assertRequestEquals(request, "server1", "server2", "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE,  RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retryTimeoutException() throws Exception {
    createHttpClientFactory("max_tries=3 max_fails=3 max_timeout_tries=1 | server=http://server1 | server=http://server2");

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

  @Test
  public void retry503ForNonIdempotentRequest() throws Exception {
    createHttpClientFactory(
      "max_tries=3 max_fails=2 retry_policy=non_idempotent_503 | server=http://server1 | server=http://server2 | server=http://server3"
    );

    Request[] request = mockRequestWith503Response();

    getTestClient().post();

    assertRequestEquals(request, "server1", "server2", "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE,  RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retryConnectTimeoutException() throws Exception {
    createHttpClientFactory("max_tries=3 max_fails=2 | server=http://server1 | server=http://server2 | server=http://server3");

    Request[] request = mockRequestWithConnectTimeoutResponse();

    getTestClient().get();

    assertRequestEquals(request, "server1", "server2", "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retryConnectTimeoutExceptionForNonIdempotentRequest() throws Exception {
    createHttpClientFactory("max_tries=3 max_fails=2 | server=http://server1 | server=http://server2 | server=http://server3");

    Request[] request = mockRequestWithConnectTimeoutResponse();

    getTestClient().post();

    assertRequestEquals(request, "server1", "server2", "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  void createHttpClientFactory(String upstreamConfig) {
    http = createHttpClientFactory(httpClient, singletonMap(TEST_UPSTREAM, upstreamConfig), null, false);
  }

  void createHttpClientFactory(String upstreamConfig, String datacenter, boolean allowCrossDCRequests) {
    http = createHttpClientFactory(httpClient, singletonMap(TEST_UPSTREAM, upstreamConfig), datacenter, allowCrossDCRequests);
  }

  void createHttpClientFactory(Map<String, String> upstreamConfigs, String datacenter, boolean allowCrossDCRequests) {
    http = createHttpClientFactory(httpClient, upstreamConfigs, datacenter, allowCrossDCRequests);
  }

  Request completeWith(int status, InvocationOnMock iom) throws Exception {
    Response response = mock(Response.class);

    when(response.getStatusCode()).thenReturn(status);
    when(response.getHeader(eq(HttpHeaders.CONTENT_TYPE))).thenReturn(MediaType.PLAIN_TEXT_UTF_8.toString());

    Request request = iom.getArgument(0);
    CompletionHandler handler = iom.getArgument(1);
    handler.onCompleted(response);
    return request;
  }

  static void assertHostEquals(Request request, String host) {
    assertEquals(host, request.getUri().getHost());
  }
  static void assertRequestTimeoutEquals(Request request, long timeoutMs) {
    assertEquals(request.getRequestTimeout(), (int) timeoutMs);
  }

  private HttpClientFactory createHttpClientFactory(AsyncHttpClient httpClient, Map<String, String> upstreamConfigs, String datacenter,
                                                    boolean allowCrossDCRequests) {
    Monitoring monitoring = mock(Monitoring.class);
    upstreamManager = new BalancingUpstreamManager(
      upstreamConfigs, newSingleThreadScheduledExecutor(), Set.of(monitoring), datacenter, allowCrossDCRequests) {
      @Override
      protected LocalDateTime getNow() {
        return BalancingClientTestBase.this.getNow();
      }
    };
    return new HttpClientFactory(httpClient, singleton("http://" + TEST_UPSTREAM),
        new SingletonStorage<>(() -> httpClientContext), Runnable::run, upstreamManager);
  }

  Request failWith(Throwable t, InvocationOnMock iom) {
    Request request = iom.getArgument(0);
    CompletionHandler handler = iom.getArgument(1);
    handler.onThrowable(t);
    return request;
  }

  protected abstract boolean isAdaptive();
  protected LocalDateTime getNow() {
    return LocalDateTime.now();
  }

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
    return new TestClient(http, isAdaptive());
  }

  static class TestClient extends JClientBase {
    private final boolean adaptive;
    private String profile;

    TestClient(HttpClientFactory http, boolean adaptive) {
      super("http://" + TEST_UPSTREAM, http);
      this.adaptive = adaptive;
    }

    public TestClient withProfile(String profile) {
      this.profile = profile;
      return this;
    }

    void get() throws Exception {
      ru.hh.jclient.common.Request request = get(url("/get")).build();
      HttpClient client = http.with(request);
      if (profile != null) {
        client = client.withProfile(profile);
      }
      if (adaptive) {
        client = client.adaptive();
      }
      client.expectPlainText().result().get();
    }

    void post() throws Exception {
      ru.hh.jclient.common.Request request = post(url("/post")).build();
      HttpClient client = http.with(request);
      if (profile != null) {
        client = client.withProfile(profile);
      }
      if (adaptive) {
        client = client.adaptive();
      }
      client.expectPlainText().result().get();
    }
  }
}
