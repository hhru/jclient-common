package ru.hh.jclient.common.balancing;

import org.junit.Before;
import org.junit.Test;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.common.JClientBase;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.Response;

import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BalancingStrategyGracefulShutdownServerTest extends AbstractBalancingStrategyTest {

  private String workingServerAddress;
  private String gracefullyClosingServerAddress;
  private ConcurrentMap<String, List<Integer>> requestRouteTracking;
  private HttpClientFactory httpClientFactory;

  @Before
  public void setUp() {
    requestRouteTracking = new ConcurrentHashMap<>();
    workingServerAddress = createNormallyWorkingServer();
    gracefullyClosingServerAddress = createGracefullyClosingServer();
    httpClientFactory = buildBalancingFactory(
      TEST_UPSTREAM,
      new TestStoreFromAddress(DATACENTER, Map.of(1, List.of(gracefullyClosingServerAddress, workingServerAddress))),
      requestRouteTracking
    );
  }

  @Test
  public void testGracefulShutdownIdempotentRetries() throws ExecutionException, InterruptedException {
    Request request = new RequestBuilder(JClientBase.HTTP_GET).setUrl("http://" + TEST_UPSTREAM).build();
    Response response = httpClientFactory.with(request).unconverted().get();
    assertEquals(HttpStatuses.OK, response.getStatusCode());
    assertEquals(1, requestRouteTracking.get(gracefullyClosingServerAddress).size());
    assertEquals(HttpStatuses.CONNECT_TIMEOUT_ERROR, requestRouteTracking.get(gracefullyClosingServerAddress).get(0).intValue());
  }

  @Test
  public void testGracefulShutdownNonIdempotentNoRetry() throws ExecutionException, InterruptedException {
    Request request = new RequestBuilder(JClientBase.HTTP_POST).setUrl("http://" + TEST_UPSTREAM).build();
    Response response = httpClientFactory.with(request).unconverted().get();
    assertEquals(HttpStatuses.CONNECT_TIMEOUT_ERROR, response.getStatusCode());
    assertNull(requestRouteTracking.get(workingServerAddress));
  }

  private static String createGracefullyClosingServer() {
    return createServer(sock -> {
      try (Socket socket = sock;
           var inputStream = socket.getInputStream()
      ) {
       //to not eliminate read
       OutputStream.nullOutputStream().write(startRead(inputStream));
      }
    });
  }
}
