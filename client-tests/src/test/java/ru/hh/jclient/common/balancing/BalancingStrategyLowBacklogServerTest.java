package ru.hh.jclient.common.balancing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.common.JClientBase;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.Response;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;

public class BalancingStrategyLowBacklogServerTest extends AbstractBalancingStrategyTest {

  private String workingServerAddress;
  private String lowBacklogServerAddress;
  private ConcurrentMap<String, List<Integer>> requestRouteTracking;
  private HttpClientFactory httpClientFactory;
  private CountDownLatch latch = new CountDownLatch(1);

  @Before
  public void setUp() throws InterruptedException {
    requestRouteTracking = new ConcurrentHashMap<>();
    workingServerAddress = createNormallyWorkingServer();
    lowBacklogServerAddress = createNotAcceptingServer(latch);
    httpClientFactory = buildBalancingFactory(
      DATACENTER, TEST_UPSTREAM,
      Map.of(1, List.of(lowBacklogServerAddress, workingServerAddress)),
      requestRouteTracking
    );
    HttpRequest req = HttpRequest.newBuilder(URI.create(lowBacklogServerAddress)).GET().build();
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(50)).build();
    AtomicBoolean inBacklog = new AtomicBoolean(true);
    while (inBacklog.get()) {
      httpClient.sendAsync(req, HttpResponse.BodyHandlers.discarding()).exceptionally(ex -> {
        if (ex.getCause() instanceof HttpConnectTimeoutException) {
          inBacklog.set(false);
        }
        return null;
      });
      Thread.sleep(50);
    }
  }

  @After
  public void tearDown() {
    latch.countDown();
  }

  @Test
  public void testLowBacklogIdempotentRetries() throws ExecutionException, InterruptedException {
    Request request = new RequestBuilder(JClientBase.HTTP_GET).setUrl("http://" + TEST_UPSTREAM).build();
    Response response = httpClientFactory.with(request).unconverted().get();
    assertEquals(HttpStatuses.OK, response.getStatusCode());
    assertEquals(1, requestRouteTracking.get(lowBacklogServerAddress).size());
    assertEquals(HttpStatuses.CONNECT_TIMEOUT_ERROR, requestRouteTracking.get(lowBacklogServerAddress).get(0).intValue());
  }

  @Test
  public void testLowBacklogNonIdempotentRetries() throws ExecutionException, InterruptedException {
    Request request = new RequestBuilder(JClientBase.HTTP_POST).setUrl("http://" + TEST_UPSTREAM).build();
    Response response = httpClientFactory.with(request).unconverted().get();
    assertEquals(HttpStatuses.OK, response.getStatusCode());
    assertEquals(1, requestRouteTracking.get(lowBacklogServerAddress).size());
    assertEquals(HttpStatuses.CONNECT_TIMEOUT_ERROR, requestRouteTracking.get(lowBacklogServerAddress).get(0).intValue());
  }

  private static String createNotAcceptingServer(CountDownLatch latch) {
    Exchanger<Integer> portHolder = new Exchanger<>();
    var thread = new Thread(() -> {
      try (ServerSocket ss = new ServerSocket(0, 1)) {
        portHolder.exchange(ss.getLocalPort());
        latch.await();
      } catch (Exception e) {
        if (e instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
        throw new RuntimeException(e);
      }
    });
    thread.setDaemon(true);
    thread.start();
    return tryGetAddress(portHolder);
  }
}
