package ru.hh.jclient.common;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import org.jboss.netty.channel.ConnectTimeoutException;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.invocation.InvocationOnMock;
import ru.hh.jclient.common.HttpClientImpl.CompletionHandler;
import static ru.hh.jclient.common.TestRequestDebug.Call.FINISHED;
import static ru.hh.jclient.common.TestRequestDebug.Call.REQUEST;
import static ru.hh.jclient.common.TestRequestDebug.Call.RESPONSE;
import static ru.hh.jclient.common.TestRequestDebug.Call.RESPONSE_CONVERTED;
import static ru.hh.jclient.common.TestRequestDebug.Call.RETRY;
import ru.hh.jclient.common.balancing.BalancingUpstreamManager;
import ru.hh.jclient.common.util.storage.SingletonStorage;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class HttpClientBalancedTest extends HttpClientTestBase {

  private static final String TEST_UPSTREAM = "backend";
  private AsyncHttpClient httpClient;
  private UpstreamManager upstreamManager;

  @Parameterized.Parameters
  public static Iterable<?> data() {
    return Arrays.asList(true, false);
  }

  @Parameterized.Parameter
  public boolean adaptive;

  @Before
  public void setUp() {
    withEmptyContext();
    httpClient = mock(AsyncHttpClient.class);
    debug.reset();
  }

  @Test
  public void request() throws Exception {
    createHttpClientBuilder("| server=http://server");

    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });

    getTestClient(adaptive).get();

    assertHostEquals(request[0], "server");

    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void requestAndUpdateServers() throws Exception {
    createHttpClientBuilder("| server=http://server1 | server=http://server2");

    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });

    getTestClient().get();
    assertHostEquals(request[0], "server1");

    upstreamManager.updateUpstream(TEST_UPSTREAM, "| server=http://server2");
    getTestClient().get();
    assertHostEquals(request[0], "server2");

    upstreamManager.updateUpstream(TEST_UPSTREAM, "| server=http://server2 | server=http://server3");
    getTestClient().get();
    assertHostEquals(request[0], "server2");

    getTestClient().get();
    assertHostEquals(request[0], "server3");
  }

  @Test
  public void retryConnectTimeoutException() throws Exception {
    createHttpClientBuilder("max_tries=3 max_fails=2 | server=http://server1 | server=http://server2 | server=http://server3");

    Request[] request = mockRequestWithConnectTimeoutResponse();

    getTestClient().get();

    assertRequestsEquals(request, "server1", "server2", "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retryConnectTimeoutExceptionForNonIdempotentRequest() throws Exception {
    createHttpClientBuilder("max_tries=3 max_fails=2 | server=http://server1 | server=http://server2 | server=http://server3");

    Request[] request = mockRequestWithConnectTimeoutResponse();

    getTestClient().post();

    assertRequestsEquals(request, "server1", "server2", "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retryConnectException() throws Exception {
    createHttpClientBuilder("max_tries=3 max_fails=2 | server=http://server1 | server=http://server2 | server=http://server3");

    Request[] request = new Request[3];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = failWith(new ConnectException("Connection refused"), iom);
          return null;
        })
        .then(iom -> {
          request[1] = failWith(new ConnectException("Connection refused"), iom);
          return null;
        })
        .then(iom -> {
          request[2] = completeWith(200, iom);
          return null;
        });

    getTestClient().get();

    assertRequestsEquals(request, "server1", "server2", "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test(expected = ExecutionException.class)
  public void retryShouldFailIfNoServerAvailable() throws Exception {
    createHttpClientBuilder("max_tries=2 max_fails=1 fail_timeout_sec=0.01 | server=http://server");

    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          failWith(new TimeoutException("Request timed out 1"), iom);
          return null;
        })
        .then(iom -> {
          failWith(new TimeoutException("Request timed out 2"), iom);
          return null;
        });

    getTestClient(adaptive).get();
  }

  @Test
  public void retryTimeoutException() throws Exception {
    createHttpClientBuilder("max_tries=3 max_fails=3 max_timeout_tries=1 | server=http://server1 | server=http://server2");

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

    assertRequestsEquals(request, "server1", "server2");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retry503() throws Exception {
    createHttpClientBuilder("max_tries=3 max_fails=2 | server=http://server1 | server=http://server2 | server=http://server3");

    Request[] request = mockRequestWith503Response();

    getTestClient().get();

    assertRequestsEquals(request, "server1", "server2", "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE,  RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retry503ForNonIdempotentRequest() throws Exception {
    createHttpClientBuilder("max_tries=3 max_fails=2 retry_policy=non_idempotent_503 | server=http://server1 | server=http://server2 | server=http://server3");

    Request[] request = mockRequestWith503Response();

    getTestClient().post();

    assertRequestsEquals(request, "server1", "server2", "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE,  RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retryIOExceptionRequestClosed() throws Exception {
    createHttpClientBuilder("| server=http://server1 | server=http://server2");

    Request[] request = new Request[2];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = failWith(new IOException("Remotely closed"), iom);
          return null;
        })
        .then(iom -> {
          request[1] = completeWith(200, iom);
          return null;
        });

    getTestClient(adaptive).get();

    assertRequestsEquals(request, "server1", "server2");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  private void assertRequestsEquals(Request[] request, String... actual) {
    if (adaptive) {
      assertTrue(toSet(request).containsAll(toSet(actual)));
      assertTrue(toSet(actual).containsAll(toSet(request)));
    } else {
      assertEquals(request.length, actual.length);
      for (int i = 0; i < request.length; i++) {
        assertHostEquals(request[i], actual[i]);
      }
    }
  }

  private static <T> Set<T> toSet(T[] array) {
    return new HashSet<>(Arrays.asList(array));
  }

  private static Set<String> toSet(Request[] requests) {
    return Arrays.stream(requests).map(r -> r.getUri().getHost()).collect(Collectors.toSet());
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

  private static void assertHostEquals(Request request, String host) {
    assertEquals(host, request.getUri().getHost());
  }

  private Request failWith(Throwable t, InvocationOnMock iom) {
    Request request = iom.getArgumentAt(0, Request.class);
    CompletionHandler handler = iom.getArgumentAt(1, CompletionHandler.class);
    handler.onThrowable(t);
    return request;
  }

  private Request completeWith(int status, InvocationOnMock iom) throws Exception {
    Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(status);
    when(response.getHeader(eq(HttpHeaders.CONTENT_TYPE))).thenReturn(MediaType.PLAIN_TEXT_UTF_8.toString());
    Request request = iom.getArgumentAt(0, Request.class);
    CompletionHandler handler = iom.getArgumentAt(1, CompletionHandler.class);
    handler.onCompleted(response);
    return request;
  }

  private void createHttpClientBuilder(String upstreamConfig) {
    http = createHttpClientBuilder(httpClient, singletonMap(TEST_UPSTREAM, upstreamConfig));
  }

  private static TestClient getTestClient() {
    return new TestClient("http://" + TEST_UPSTREAM, http, false);
  }

  private static TestClient getTestClient(boolean adaptive) {
    return new TestClient("http://" + TEST_UPSTREAM, http, adaptive);
  }

  private static class TestClient extends JClientBase {
    private boolean adaptive;

    TestClient(String host, HttpClientBuilder http, boolean adaptive) {
      super(host, http);
      this.adaptive = adaptive;
    }

    void get() throws Exception {
      ru.hh.jclient.common.Request request = get(url("/get")).build();
      (adaptive ? http.withAdaptive(request) : http.with(request)).expectPlainText().result().get();
    }

    void post() throws Exception {
      ru.hh.jclient.common.Request request = post(url("/post")).build();
      (adaptive ? http.withAdaptive(request) : http.with(request)).expectPlainText().result().get();
    }
  }

  HttpClientBuilder createHttpClientBuilder(AsyncHttpClient httpClient, Map<String, String> upstreamConfigs) {
    upstreamManager = new BalancingUpstreamManager(upstreamConfigs, newSingleThreadScheduledExecutor(), mock(Monitoring.class));
    return new HttpClientBuilder(httpClient, singleton("http://" + TEST_UPSTREAM),
        new SingletonStorage<>(() -> httpClientContext), Runnable::run, upstreamManager);
  }
}
