package ru.hh.jclient.common.balancing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
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
import ru.hh.jclient.common.balancing.config.Profile;

// the test uses local but real and big http load hence is not so reliable. So, for manual use at the moment
@Ignore
@RunWith(Parameterized.class)
public class BalancingConcurrencyIntegrationTest extends AbstractBalancingStrategyTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalancingConcurrencyIntegrationTest.class);

  private String server50Address;
  private String server200Address;
  private ConcurrentMap<String, List<Integer>> requestRouteTracking;
  private ConcurrentMap<String, LongAdder> retries;
  private HttpClientFactory httpClientFactory;
  private UpstreamManager upstreamManager;
  private ServerStore serverStore;

  private Server server50;
  private Server server200;

  @Parameterized.Parameters(name = "threadpool size: {0}")
  public static Collection<Object[]> parameters() {
    return List.of(new Object[] {1}, new Object[] {16}, new Object[] {32}, new Object[] {96});
  }
  @Parameterized.Parameter(0)
  public int threads;

  @Before
  public void setUp() {
    requestRouteTracking = new ConcurrentHashMap<>();
    retries = new ConcurrentHashMap<>();
    server50Address = createServer();
    server200Address = createServer();
    serverStore = new ServerStoreImpl();
    server50 = new Server(server50Address, 50, DATACENTER);
    server200 = new Server(server200Address, 200, DATACENTER);
    serverStore.updateServers(TEST_UPSTREAM, List.of(server50, server200), List.of());
    Map.Entry<HttpClientFactory, UpstreamManager> factoryAndManager = buildBalancingFactory(
        TEST_UPSTREAM,
        new Profile().setRequestTimeoutSec(100f),
        serverStore,
        requestRouteTracking,
        retries
    );
    httpClientFactory = factoryAndManager.getKey();
    upstreamManager = factoryAndManager.getValue();
  }

  @Test
  public void testStat() throws InterruptedException {
    int statLimit = 1_000;
    upstreamManager.getUpstream(TEST_UPSTREAM).setStatLimit(statLimit);
    ExecutorService executorService = Executors.newFixedThreadPool(threads);
    AtomicInteger counter = new AtomicInteger();
    executorService.invokeAll(IntStream.range(0, 10_257).mapToObj(index -> (Callable<?>) () -> {
      try {
        Request request = new RequestBuilder(JClientBase.HTTP_GET).setUrl("http://" + TEST_UPSTREAM).build();
        Response response = httpClientFactory.with(request).unconverted().get();
        assertEquals(HttpStatuses.OK, response.getStatusCode());
        LOGGER.info("Processed requests: {}", counter.incrementAndGet());
        return null;
      } catch (ExecutionException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toList()));
    List<Server> servers = serverStore.getServers(TEST_UPSTREAM);

    int minWeight = servers.stream().mapToInt(Server::getWeight).min().getAsInt();
    int sumWeight = servers.stream().mapToInt(Server::getWeight).sum();
    int totalUserRequests = requestRouteTracking.entrySet().stream()
      .mapToInt(addressToReponseCodes -> {
        int retries = this.retries.getOrDefault(addressToReponseCodes.getKey(), new LongAdder()).intValue();
        return addressToReponseCodes.getValue().size() - retries;
      })
      .sum();
    for (Server server : servers) {
      double weightPart = (double) server.getWeight() / sumWeight;
      double userRequestsHandled = requestRouteTracking.get(server.getAddress()).size()
        - retries.getOrDefault(server.getAddress(), new LongAdder()).intValue();
      double requestsHandledPart = userRequestsHandled / totalUserRequests;
      LOGGER.info("Server {}: weightPart={}, requestsHandledPart={}", server, weightPart, requestsHandledPart);
      assertEquals(weightPart, requestsHandledPart, 0.01);
      assertTrue("Weight: " + server.getWeight(), server.getStatsRequests() <= ((double) server.getWeight() / minWeight) * statLimit);
    }
  }

  private String createServer() {
    return createServer(socket -> {
      try (socket;
           var inputStream = socket.getInputStream();
           var in = new BufferedReader(new InputStreamReader(inputStream));
           var output = new PrintWriter(socket.getOutputStream())
      ) {
        String arg;
        do {
          arg = in.readLine();
        } while (arg != null && !arg.isEmpty());
        output.println("HTTP/1.1 200 OK");
        output.println("");
        output.flush();
      }
    }, threads * 10);
  }
}
