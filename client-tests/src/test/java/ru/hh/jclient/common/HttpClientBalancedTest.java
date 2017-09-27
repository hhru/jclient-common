package ru.hh.jclient.common;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import static java.util.Collections.singletonMap;
import org.jboss.netty.channel.ConnectTimeoutException;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import ru.hh.jclient.common.HttpClientImpl.CompletionHandler;
import static ru.hh.jclient.common.TestRequestDebug.Call.FINISHED;
import static ru.hh.jclient.common.TestRequestDebug.Call.REQUEST;
import static ru.hh.jclient.common.TestRequestDebug.Call.RESPONSE;
import static ru.hh.jclient.common.TestRequestDebug.Call.RESPONSE_CONVERTED;
import static ru.hh.jclient.common.TestRequestDebug.Call.RETRY;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class HttpClientBalancedTest extends HttpClientTestBase {

  private static final String TEST_HOST = "backend";
  private AsyncHttpClient httpClient;

  @Before
  public void setUp() throws Exception {
    withEmptyContext();
    httpClient = mock(AsyncHttpClient.class);
    debug.reset();
  }

  @Test
  public void okRequest() throws Exception {
    createHttpClientBuilder("| server=http://server");

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
  public void retryConnectTimeoutShouldFailIfNoServerAvailable() throws Exception {
    createHttpClientBuilder("max_tries=2 max_fails=1 fail_timeout_sec=0.01 | server=http://server");

    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          failWith(new ConnectTimeoutException("Connect timed out 1"), iom);
          return null;
        })
        .then(iom -> {
          failWith(new ConnectTimeoutException("Connect timed out 2"), iom);
          return null;
        });

    getTestClient().get();
  }

  @Test
  public void retryConnectTimeoutForMultipleServers() throws Exception {
    createHttpClientBuilder("max_tries=3 max_fails=2 | server=http://server1 | server=http://server2 | server=http://server3");

    Request[] request = mockConnectTimeoutRequest();

    getTestClient().get();

    assertHostEquals(request[0], "server1");
    assertHostEquals(request[1], "server2");
    assertHostEquals(request[2], "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retryConnectTimeoutForNonIdempotentRequest() throws Exception {
    createHttpClientBuilder("max_tries=3 max_fails=2 | server=http://server1 | server=http://server2 | server=http://server3");

    Request[] request = mockConnectTimeoutRequest();

    getTestClient().post();

    assertHostEquals(request[0], "server1");
    assertHostEquals(request[1], "server2");
    assertHostEquals(request[2], "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test(expected = ExecutionException.class)
  public void retryRequestTimeoutShouldFailIfNoServerAvailable() throws Exception {
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

    getTestClient().get();
  }

  @Test(expected = ExecutionException.class)
  public void requestTimeoutShouldNotBeRetriedForNonIdempotentRequest() throws Exception {
    createHttpClientBuilder("max_tries=2 max_fails=2 fail_timeout_sec=0.01 | server=http://server");

    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          failWith(new TimeoutException("Request timed out 1"), iom);
          return null;
        });

    getTestClient().post();
  }

  @Test
  public void retryRequestTimeout() throws Exception {
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

    assertHostEquals(request[0], "server1");
    assertHostEquals(request[1], "server2");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void retryRequestTimeoutBy503Response() throws Exception {
    createHttpClientBuilder("max_tries=3 max_fails=2 | server=http://server1 | server=http://server2 | server=http://server3");

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

    getTestClient().get();

    assertHostEquals(request[0], "server1");
    assertHostEquals(request[1], "server2");
    assertHostEquals(request[2], "server3");

    debug.assertCalled(REQUEST, RESPONSE, RETRY, RESPONSE,  RETRY, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  private Request[] mockConnectTimeoutRequest() {
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
    http = createHttpClientBuilder(httpClient, singletonMap(TEST_HOST, upstreamConfig));
  }

  private static TestClient getTestClient() {
    return new TestClient("http://" + TEST_HOST, http);
  }

  private static class TestClient extends AbstractClient {
    TestClient(String host, HttpClientBuilder http) {
      super(host, http);
    }

    String get() throws Exception {
      Request request = get(url("/get"), "param1", "value1").build();
      return http.with(request).expectPlainText().result().get();
    }

    String post() throws Exception {
      Request request = post(url("/post"), "param1", "value1").build();
      return http.with(request).expectPlainText().result().get();
    }
  }
}
