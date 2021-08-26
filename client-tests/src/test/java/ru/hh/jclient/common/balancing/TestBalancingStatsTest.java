package ru.hh.jclient.common.balancing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.common.JClientBase;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.Response;

//flacking concurrency test - for manual run
@RunWith(Parameterized.class)
@Ignore
public class TestBalancingStatsTest extends AbstractBalancingStrategyTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestBalancingStatsTest.class);

  private String server50Address;
  private String server200Address;
  private ConcurrentMap<String, List<Integer>> requestRouteTracking;
  private HttpClientFactory httpClientFactory;
  private ServerStore serverStore;

  private Server server50;
  private Server server200;

  @Parameterized.Parameters(name = "threadpool size: {0}")
  public static Collection<Object[]> parameters() {
    return List.of(new Object[] {16}, new Object[] {32}, new Object[] {96});
  }
  @Parameterized.Parameter
  public int threads;

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
  public void testStat() throws InterruptedException {
    ExecutorService executorService = Executors.newFixedThreadPool(threads);
    AtomicInteger counter = new AtomicInteger();
    executorService.invokeAll(IntStream.range(0, 10_257).mapToObj(index -> (Callable<?>) () -> {
      try {
        Request request = new RequestBuilder(JClientBase.HTTP_GET).setUrl("http://" + TEST_UPSTREAM).build();
        Response response = httpClientFactory.with(request).unconverted().get();
        assertEquals(HttpStatuses.OK, response.getStatusCode());
        Thread.sleep(5);
        LOGGER.info("Processed requests: {}", counter.incrementAndGet());
        return null;
      } catch (ExecutionException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toList()));
    List<Server> servers = serverStore.getServers(TEST_UPSTREAM);

    int maxWeight = servers.stream().mapToInt(Server::getWeight).max().getAsInt();
    int sumWeight = servers.stream().mapToInt(Server::getWeight).sum();
    int totalRequests = requestRouteTracking.values().stream().mapToInt(Collection::size).sum();
    for (Server server : servers) {
      double weightPart = (double) server.getWeight() / sumWeight;
      double requestsHandledPart = (double) requestRouteTracking.get(server.getAddress()).size() / totalRequests;
      LOGGER.info("Server {}: weightPart={}, requestsHandledPart={}", server.getAddress(), weightPart, requestsHandledPart);
      assertEquals(weightPart, requestsHandledPart, 0.01);
      assertTrue(server.getAddress(), server.getStatsRequests() <= maxWeight);
    }
  }

  private String createServer() {
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
        Thread.sleep(5);
        output.println("HTTP/1.1 200 OK");
        output.println("");
        output.flush();
      }
    }, threads * 10);
  }
}
