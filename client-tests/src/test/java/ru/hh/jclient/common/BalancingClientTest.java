package ru.hh.jclient.common;

import org.asynchttpclient.Request;
import org.junit.Test;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;
import ru.hh.jclient.common.HttpClientImpl.CompletionHandler;
import static ru.hh.jclient.common.TestRequestDebug.Call.FINISHED;
import static ru.hh.jclient.common.TestRequestDebug.Call.REQUEST;
import static ru.hh.jclient.common.TestRequestDebug.Call.RESPONSE;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class BalancingClientTest extends BalancingClientTestBase {

  @Test
  public void requestAndUpdateServers() throws Exception {
    createHttpClientFactory("| server=http://server1 | server=http://server2");

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
  public void shouldNotRetryRequestTimeoutForPost() {
    createHttpClientFactory("max_tries=3 max_fails=4 max_timeout_tries=2 " +
        "| server=http://server1 | server=http://server2");

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
    createHttpClientFactory("| server=http://server1 dc=DC1 | server=http://server2 dc=DC2", "DC1", false);

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

  @Test(expected = ExecutionException.class)
  public void failIfNoBackendAvailableInCurrentDC() throws Exception {
    createHttpClientFactory("| server=http://server1 dc=DC2 | server=http://server2 dc=DC2", "DC1", false);
    getTestClient().get();
  }

  @Test
  public void testAllowCrossDCRequests() throws Exception {
    createHttpClientFactory("| server=http://server1 dc=DC1 | server=http://server2 dc=DC2", "DC1", true);

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

    upstreamManager.updateUpstream(TEST_UPSTREAM, "| server=http://server2 dc=DC2");

    getTestClient().get();
    assertHostEquals(request[0], "server2");
  }

  @Override
  public boolean isAdaptive() {
    return false;
  }
}
