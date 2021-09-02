package ru.hh.jclient.common.balancing;

import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.Response;

public class BalancingStrategyResettingServerTest extends AbstractBalancingStrategyTest {

  private String workingServerAddress;
  private String resettingServerAddress;
  private ConcurrentMap<String, List<Integer>> requestRouteTracking;
  private HttpClientFactory httpClientFactory;

  @Before
  public void setUp() {
    requestRouteTracking = new ConcurrentHashMap<>();
    workingServerAddress = createNormallyWorkingServer();
    resettingServerAddress = createResettingServer();
    httpClientFactory = buildBalancingFactory(
      TEST_UPSTREAM,
      new TestStoreFromAddress(DATACENTER, Map.of(1, List.of(resettingServerAddress, workingServerAddress))),
      requestRouteTracking
    ).getKey();
  }

  @Test
  public void testConnectionResetIdempotentRetries() throws ExecutionException, InterruptedException {
    Response response = httpClientFactory.with(new RequestBuilder("GET").setUrl("http://" + TEST_UPSTREAM).build()).unconverted().get();
    assertEquals(HttpStatuses.OK, response.getStatusCode());
    assertEquals(1, requestRouteTracking.get(resettingServerAddress).size());
    assertEquals(HttpStatuses.CONNECT_TIMEOUT_ERROR, requestRouteTracking.get(resettingServerAddress).get(0).intValue());
  }

  @Test
  public void testConnectionResetNonIdempotentNoRetry() throws ExecutionException, InterruptedException {
    Response response = httpClientFactory.with(new RequestBuilder("POST").setUrl("http://" + TEST_UPSTREAM).build()).unconverted().get();
    assertEquals(HttpStatuses.CONNECT_TIMEOUT_ERROR, response.getStatusCode());
    assertNull(requestRouteTracking.get(workingServerAddress));
  }

  private static String createResettingServer() {
    return createServer(sock -> {
      try (Socket socket = sock) {
        socket.setSoLinger(true, 0);
        var inputStream = socket.getInputStream();
        //to not eliminate read
        OutputStream.nullOutputStream().write(startRead(inputStream));
      }
    });
  }
}
