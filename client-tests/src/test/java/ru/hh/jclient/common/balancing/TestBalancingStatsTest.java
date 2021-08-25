package ru.hh.jclient.common.balancing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.common.JClientBase;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.Response;

//flacking concurrency test - for manual run
@Ignore
public class TestBalancingStatsTest extends AbstractBalancingStrategyTest {
  private String server50Address;
  private String server200Address;
  private ConcurrentMap<String, List<Integer>> requestRouteTracking;
  private HttpClientFactory httpClientFactory;
  private ServerStore serverStore;

  private Server server50;
  private Server server200;

  @Before
  public void setUp() {
    requestRouteTracking = new ConcurrentHashMap<>();
    server50Address = createServer();
    server200Address = createServer();
    serverStore = new ServerStoreImpl();
    server50 = new Server(server50Address, 50, DATACENTER);
    server200 = new Server(server200Address, 200, DATACENTER);
    serverStore.updateServers(TEST_UPSTREAM, List.of(server50, server200), List.of());
    httpClientFactory = buildBalancingFactory(
        TEST_UPSTREAM,
        serverStore,
        requestRouteTracking
    );
  }

  @Test
  public void testStat() throws ExecutionException, InterruptedException {
    ExecutorService executorService = Executors.newFixedThreadPool(16);
    executorService.invokeAll(IntStream.range(0, 1000).mapToObj(index -> (Callable<?>) () -> {
      try {
        Request request = new RequestBuilder(JClientBase.HTTP_GET).setUrl("http://" + TEST_UPSTREAM).build();
        Response response = httpClientFactory.with(request).unconverted().get();
        assertEquals(HttpStatuses.OK, response.getStatusCode());
        Thread.sleep(10);
        return null;
      } catch (ExecutionException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toList()));
    List<Server> servers = serverStore.getServers(TEST_UPSTREAM);
    long approxRatio = Math.round((double) requestRouteTracking.get(server200Address).size() / requestRouteTracking.get(server50Address).size());
    assertEquals(4.0, approxRatio, 0.0001);
    for (Server server : servers) {
      assertTrue(server.getAddress(), server.getStatsRequests() <= server200.getStatsRequests());
    }
  }

  private static String createServer() {
    return createServer(sock -> {
      try (Socket socket = sock;
           var inputStream = socket.getInputStream();
           var in = new BufferedReader(new InputStreamReader(inputStream));
           var output = new PrintWriter(socket.getOutputStream())
      ) {
        String arg;
        do {
          arg = in.readLine();
        } while (arg != null && !arg.isEmpty());
        Thread.sleep(50);
        output.println("HTTP/1.1 200 OK");
        output.println("");
        output.flush();
      }
    });
  }
}
