package ru.hh.jclient.common.balancing;

import org.junit.Before;
import org.junit.Test;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.common.JClientBase;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.Response;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import ru.hh.jclient.common.balancing.config.Profile;

public class BalancingStrategyExceptionallyShutdownServerTest extends AbstractBalancingStrategyTest {

  private String workingServerAddress;
  private String exceptionallyClosingServerAddress;
  private ConcurrentMap<String, List<Integer>> requestRouteTracking;
  private HttpClientFactory httpClientFactory;

  @Before
  public void setUp() {
    requestRouteTracking = new ConcurrentHashMap<>();
    workingServerAddress = createNormallyWorkingServer();
    exceptionallyClosingServerAddress = createExceptionallyStoppingServer();
    httpClientFactory = buildBalancingFactory(
      TEST_UPSTREAM,
      new Profile().setMaxTimeoutTries(2),
      new TestStoreFromAddress(DATACENTER, Map.of(1, List.of(exceptionallyClosingServerAddress, workingServerAddress))),
      requestRouteTracking
    );
  }

  @Test
  public void testForceShutdownIdempotentRetries() throws ExecutionException, InterruptedException {
    Request request = new RequestBuilder(JClientBase.HTTP_GET).setUrl("http://" + TEST_UPSTREAM).build();
    Response response = httpClientFactory.with(request).unconverted().get();
    assertEquals(HttpStatuses.OK, response.getStatusCode());
    assertEquals(1, requestRouteTracking.get(exceptionallyClosingServerAddress).size());
    assertEquals(HttpStatuses.CONNECT_TIMEOUT_ERROR, requestRouteTracking.get(exceptionallyClosingServerAddress).get(0).intValue());
  }

  @Test
  public void testForceShutdownNonIdempotentNoRetry() throws ExecutionException, InterruptedException {
    Request request = new RequestBuilder(JClientBase.HTTP_POST).setUrl("http://" + TEST_UPSTREAM).build();
    Response response = httpClientFactory.with(request).unconverted().get();
    assertEquals(HttpStatuses.CONNECT_TIMEOUT_ERROR, response.getStatusCode());
    assertNull(requestRouteTracking.get(workingServerAddress));
  }

  private static String createExceptionallyStoppingServer() {
    Exchanger<Integer> portHolder = new Exchanger<>();
    var thread = new Thread(() -> {
      try (ServerSocket ss = new ServerSocket(0)) {
        portHolder.exchange(ss.getLocalPort());
        Socket socket = ss.accept();
        var inputStream = socket.getInputStream();
        //to not eliminate read
        OutputStream.nullOutputStream().write(startRead(inputStream));
        throw new RuntimeException("Exceptionally finishing server");
      } catch (IOException | InterruptedException e) {
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
