package ru.hh.jclient.common.balancing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
@Disabled
public class BalancingConcurrencyIntegrationTest extends AbstractBalancingStrategyTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalancingConcurrencyIntegrationTest.class);

  @ParameterizedTest(name = "threadpool size: {0}")
  @ValueSource(ints = {1, 16, 32, 96})
  public void testStat(int threads) throws InterruptedException {
    ConcurrentMap<String, List<Integer>> requestRouteTracking = new ConcurrentHashMap<>();
    ConcurrentMap<String, LongAdder> retries = new ConcurrentHashMap<>();
    String server50Address = createServer(threads);
    String server200Address = createServer(threads);
    ServerStore serverStore = new ServerStoreImpl();
    Server server50 = new Server(server50Address, null, 50, DATACENTER);
    Server server200 = new Server(server200Address, null, 200, DATACENTER);
    serverStore.updateServers(TEST_UPSTREAM, List.of(server50, server200), List.of());
    Map.Entry<HttpClientFactory, UpstreamManager> factoryAndManager = buildBalancingFactory(
        TEST_UPSTREAM,
        new Profile().setRequestTimeoutSec(100f),
        serverStore,
        requestRouteTracking,
        retries
    );
    HttpClientFactory httpClientFactory = factoryAndManager.getKey();
    UpstreamManager upstreamManager = factoryAndManager.getValue();

    int statLimit = 1_000;
    upstreamManager.getUpstream(TEST_UPSTREAM).setStatLimit(statLimit);
    ExecutorService executorService = Executors.newFixedThreadPool(threads);
    AtomicInteger counter = new AtomicInteger();
    executorService.invokeAll(
        IntStream
            .range(0, 10_257)
            .mapToObj(index -> (Callable<?>) () -> {
              try {
                Request request = new RequestBuilder(JClientBase.HTTP_GET).setUrl("http://" + TEST_UPSTREAM).build();
                Response response = httpClientFactory.with(request).unconverted().get();
                assertEquals(HttpStatuses.OK, response.getStatusCode());
                LOGGER.info("Processed requests: {}", counter.incrementAndGet());
                return null;
              } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
              }
            })
            .collect(Collectors.toList())
    );
    List<Server> servers = serverStore.getServers(TEST_UPSTREAM);

    int minWeight = servers.stream().mapToInt(Server::getWeight).min().getAsInt();
    int sumWeight = servers.stream().mapToInt(Server::getWeight).sum();
    int totalUserRequests = requestRouteTracking
        .entrySet()
        .stream()
        .mapToInt(addressToResponseCodes ->
            addressToResponseCodes.getValue().size() - retries.getOrDefault(addressToResponseCodes.getKey(), new LongAdder()).intValue()
        )
        .sum();
    for (Server server : servers) {
      double weightPart = (double) server.getWeight() / sumWeight;
      double userRequestsHandled = requestRouteTracking.get(server.getAddress()).size()
          - retries.getOrDefault(server.getAddress(), new LongAdder()).intValue();
      double requestsHandledPart = userRequestsHandled / totalUserRequests;
      LOGGER.info("Server {}: weightPart={}, requestsHandledPart={}", server, weightPart, requestsHandledPart);
      assertEquals(weightPart, requestsHandledPart, 0.01);
      assertTrue(server.getStatsRequests() <= ((double) server.getWeight() / minWeight) * statLimit, () -> "Weight: " + server.getWeight());
    }
  }

  private String createServer(int threads) {
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
