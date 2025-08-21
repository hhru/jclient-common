package ru.hh.jclient.common.balancing;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.common.JClientBase;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.balancing.config.Profile;
import ru.hh.jclient.common.balancing.config.RetryPolicyConfig;

public class BalancingStrategyExceptionallyShutdownServerTest extends AbstractBalancingStrategyTest {

  private String workingServerAddress;
  private String exceptionallyClosingServerAddress;
  private ConcurrentMap<String, List<Integer>> requestRouteTracking;
  private HttpClientFactory httpClientFactory;

  @BeforeEach
  public void setUp() {
    requestRouteTracking = new ConcurrentHashMap<>();
    workingServerAddress = createNormallyWorkingServer();
    exceptionallyClosingServerAddress = createExceptionallyStoppingServer();
    httpClientFactory = buildBalancingFactory(
      TEST_UPSTREAM,
      new Profile().setMaxTimeoutTries(2).setRetryPolicy(Map.of(577, new RetryPolicyConfig().setRetryNonIdempotent(false))),
      new TestStoreFromAddress(DATACENTER, Map.of(1, List.of(exceptionallyClosingServerAddress, workingServerAddress))),
      requestRouteTracking, null
    ).getKey();
  }

  @Test
  public void testForceShutdownIdempotentRetries() throws ExecutionException, InterruptedException {
    Request request = new RequestBuilder(JClientBase.HTTP_GET).setUrl("http://" + TEST_UPSTREAM).build();
    Response response = httpClientFactory.with(request).unconverted().get();
    assertEquals(HttpStatuses.OK, response.getStatusCode());
    assertEquals(1, requestRouteTracking.get(exceptionallyClosingServerAddress).size());
    assertEquals(HttpStatuses.SERVER_TIMEOUT, requestRouteTracking.get(exceptionallyClosingServerAddress).get(0).intValue());
  }

  @Test
  public void testForceShutdownNonIdempotentNoRetry() throws ExecutionException, InterruptedException {
    Request request = new RequestBuilder(JClientBase.HTTP_POST).setUrl("http://" + TEST_UPSTREAM).build();
    Response response = httpClientFactory.with(request).unconverted().get();
    assertEquals(HttpStatuses.SERVER_TIMEOUT, response.getStatusCode());
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
