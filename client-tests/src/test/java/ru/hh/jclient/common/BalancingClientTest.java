package ru.hh.jclient.common;

import org.asynchttpclient.Request;
import org.junit.Test;
import ru.hh.jclient.common.HttpClientImpl.CompletionHandler;
import ru.hh.jclient.common.balancing.RequestBalancerBuilder;
import ru.hh.jclient.common.balancing.Upstream.UpstreamKey;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.hh.jclient.common.TestRequestDebug.Call.FINISHED;
import static ru.hh.jclient.common.TestRequestDebug.Call.REQUEST;
import static ru.hh.jclient.common.TestRequestDebug.Call.RESPONSE;

public class BalancingClientTest extends BalancingClientTestBase {

  @Test
  public void requestWithProfile() throws Exception {
    var upstreamConfigs = Map.of(
        UpstreamKey.ofComplexName(TEST_UPSTREAM).getWholeName(),
        "request_timeout_sec=2 | server=http://server1 | server=http://server2",
        new UpstreamKey(TEST_UPSTREAM, "foo").getWholeName(),
        "request_timeout_sec=1 | server=http://server1 | server=http://server2"
    );
    createHttpClientFactory(upstreamConfigs, null, false);
    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });
    getTestClient().get();
    assertRequestTimeoutEquals(request[0], TimeUnit.SECONDS.toMillis(2));
    getTestClient().withPreconfiguredEngine(RequestBalancerBuilder.class, builder -> builder.withProfile("foo")).get();
    assertRequestTimeoutEquals(request[0], TimeUnit.SECONDS.toMillis(1));
    getTestClient().withPreconfiguredEngine(RequestBalancerBuilder.class, builder -> builder.withProfile("not existing")).get();
    assertRequestTimeoutEquals(request[0], TimeUnit.SECONDS.toMillis(2));
    getTestClient().get();
    assertRequestTimeoutEquals(request[0], TimeUnit.SECONDS.toMillis(2));
  }

  @Test(expected = ClassCastException.class)
  public void preconfiguredWithWrongClass() throws Exception {
    var upstreamConfigs = Map.of(
            UpstreamKey.ofComplexName(TEST_UPSTREAM).getWholeName(),
            "request_timeout_sec=2 | server=http://server1 | server=http://server2"
    );
    createHttpClientFactory(upstreamConfigs, null, false);
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
    var upstreamConfigs = Map.of(
            UpstreamKey.ofComplexName(TEST_UPSTREAM).getWholeName(),
            "request_timeout_sec=2 | server=http://server1 | server=http://server2"
    );
    createHttpClientFactory(upstreamConfigs, null, false);
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
    var upstreamConfigs = Map.of(
        UpstreamKey.ofComplexName(TEST_UPSTREAM).getWholeName(),
        "request_timeout_sec=6 | server=http://server6",
        new UpstreamKey(TEST_UPSTREAM, "foo").getWholeName(),
        "request_timeout_sec=4 | server=http://server4 ",
        new UpstreamKey(TEST_UPSTREAM, "bar").getWholeName(),
        "request_timeout_sec=2 | server=http://server2"
    );
    createHttpClientFactory(upstreamConfigs, null, false);
    Request[] request = new Request[1];
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
        .then(iom -> {
          request[0] = completeWith(200, iom);
          return null;
        });
    withContext(Map.of(HttpHeaderNames.X_OUTER_TIMEOUT_MS, List.of(Long.toString(TimeUnit.SECONDS.toMillis(1)))));
    getTestClient().get("http://foo/get");
    assertHostEquals(request[0], "foo");
  }

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

    requestingStrategy.getUpstreamManager().updateUpstream(TEST_UPSTREAM, "| server=http://server2");
    getTestClient().get();
    assertHostEquals(request[0], "server2");

    requestingStrategy.getUpstreamManager()
        .updateUpstream(TEST_UPSTREAM, "| server=http://server2 | server=http://server3");
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

  @Test
  public void balancedRequestMonitoring() throws Exception {
    createHttpClientFactory("| server=http://server1 dc=DC1", "DC1", false);

    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
      .then(iom -> {
        completeWith(200, iom);
        return null;
      });

    http.with(new RequestBuilder("GET").setUrl("http://backend/path?query").build())
      .expectPlainText().result().get();

    Monitoring monitoring = requestingStrategy.getUpstreamManager().getMonitoring().stream().findFirst().get();
    verify(monitoring).countRequest(
      eq("backend"), eq("DC1"), eq("http://server1"), eq(200), anyLong(), eq(true)
    );
  }

  @Test
  public void unbalancedRequestMonitoring() throws Exception {
    createHttpClientFactory("| server=http://server1 dc=DC1", "DC1", false);

    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class)))
      .then(iom -> {
        completeWith(200, iom);
        return null;
      });

    http.with(new RequestBuilder("GET").setUrl("http://not-balanced-backend/path?query").build())
      .expectPlainText().result().get();

    Monitoring monitoring = requestingStrategy.getUpstreamManager().getMonitoring().stream().findFirst().get();
    verify(monitoring).countRequest(
      eq("http://not-balanced-backend"), eq(null), eq("http://not-balanced-backend"), eq(200), anyLong(), eq(true)
    );
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

    requestingStrategy.getUpstreamManager().updateUpstream(TEST_UPSTREAM, "| server=http://server2 dc=DC2");

    getTestClient().get();
    assertHostEquals(request[0], "server2");
  }

  @Override
  public boolean isAdaptive() {
    return false;
  }
}
