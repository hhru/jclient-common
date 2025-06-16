package ru.hh.jclient.common.balancing;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.common.JClientBase;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.Response;

public class BalancingStrategyNotExistingServerTest extends AbstractBalancingStrategyTest {

  private String workingServerAddress;
  private static final String notExistingServerAddress = "http://localhost:255"; // port 255 is reserved
  private ConcurrentMap<String, List<Integer>> requestRouteTracking;
  private HttpClientFactory httpClientFactory;

  @Before
  public void setUp() {
    requestRouteTracking = new ConcurrentHashMap<>();
    workingServerAddress = createNormallyWorkingServer();
    httpClientFactory = buildBalancingFactory(
      TEST_UPSTREAM,
      new TestStoreFromAddress(DATACENTER, Map.of(1, List.of(notExistingServerAddress, workingServerAddress))),
      requestRouteTracking
    ).getKey();
  }

  @Test
  public void testNonExistentIdempotentRetries() throws ExecutionException, InterruptedException {
    Request request = new RequestBuilder(JClientBase.HTTP_GET).setUrl("http://" + TEST_UPSTREAM).build();
    Response response = httpClientFactory.with(request).unconverted().get();
    assertEquals(HttpStatuses.OK, response.getStatusCode());
    assertEquals(1, requestRouteTracking.get(notExistingServerAddress).size());
    assertEquals(HttpStatuses.CONNECT_TIMEOUT_ERROR, requestRouteTracking.get(notExistingServerAddress).get(0).intValue());
  }

  @Test
  public void testNonExistentNonIdempotentRetries() throws ExecutionException, InterruptedException {
    Request request = new RequestBuilder(JClientBase.HTTP_POST).setUrl("http://" + TEST_UPSTREAM).build();
    Response response = httpClientFactory.with(request).unconverted().get();
    assertEquals(HttpStatuses.OK, response.getStatusCode());
    assertEquals(1, requestRouteTracking.get(notExistingServerAddress).size());
    assertEquals(HttpStatuses.CONNECT_TIMEOUT_ERROR, requestRouteTracking.get(notExistingServerAddress).get(0).intValue());
  }
}
