package ru.hh.jclient.common.balancing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.collection.IsMapContaining;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.HttpClientFactoryBuilder;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.common.JClientBase;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.balancing.config.ApplicationConfig;
import ru.hh.jclient.common.balancing.config.Host;
import ru.hh.jclient.common.balancing.config.Profile;
import ru.hh.jclient.common.util.storage.SingletonStorage;
import ru.hh.jclient.consul.TestUpstreamConfigService;
import ru.hh.jclient.consul.TestUpstreamService;

// the test uses local but real and big http load hence is not so reliable. So, for manual use at the moment
@Ignore
public class BalancingWorkloadModelTest {

  private TestServerManager testServerManager;
  private ExecutorService executorService;

  @Before
  public void setUp() {
    executorService = Executors.newFixedThreadPool(64);
    testServerManager = new TestServerManager(new ServerStoreImpl(), "test", 64 * 1000);
  }

  @After
  public void tearDown() {
    executorService.shutdownNow();
  }

  @Test
  public void testHappyPass1DcBalancing() throws InterruptedException {
    String dc = "test";
    String upstreamName = "test";
    int requests = 10_257;

    testServerManager.addServer("server50", 50, dc, 0f);
    testServerManager.addServer("server150", 150, dc, 0f);
    HttpClientFactory client = testServerManager.getClient(
        dc,
        new Profile()
    );

    AtomicInteger clientRequestsCounter = new AtomicInteger();
    AtomicInteger clientErrors = new AtomicInteger();
    executorService.invokeAll(IntStream.range(0, requests)
        .mapToObj(index -> (Callable<?>) () -> executeBalancingRequest(upstreamName, client, clientRequestsCounter, clientErrors))
        .collect(Collectors.toList()));
    assertEquals(0, clientErrors.get());
    assertServerRequestDistribution(Set.of());
  }

  @Test
  public void test1DcBalancingLowWeightServerOutage() throws InterruptedException {
    String dc = "test";
    String upstreamName = "test";
    int requests = 10_257;

    String failingServerName = "server50";
    testServerManager.addServer(failingServerName, 50, dc, 0f);
    testServerManager.addServer("server150", 150, dc, 0f);
    testServerManager.addServer("server110", 110, dc, 0f);
    HttpClientFactory client = testServerManager.getClient(
        dc,
        new Profile()
    );
    AtomicInteger clientRequestsCounter = new AtomicInteger();
    AtomicInteger clientErrors = new AtomicInteger();
    executorService.invokeAll(IntStream.range(0, requests)
        .mapToObj(index -> (Callable<?>) () -> executeBalancingRequest(upstreamName, client, clientRequestsCounter, clientErrors))
        .collect(Collectors.toList()));
    assertEquals(0, clientErrors.get());
    assertServerRequestDistribution(Set.of());

    testServerManager.removeServer(failingServerName);

    executorService.invokeAll(IntStream.range(0, requests)
        .mapToObj(index -> (Callable<?>) () -> executeBalancingRequest(upstreamName, client, clientRequestsCounter, clientErrors))
        .collect(Collectors.toList()));
    assertEquals(0, clientErrors.get());
    assertServerRequestDistribution(Set.of());
  }

  @Test
  public void test1DcBalancingLowWeightServerErrors() throws InterruptedException {
    String dc = "test";
    String upstreamName = "test";
    int requests = 10_257;

    String failingServerName = "server50";
    testServerManager.addServer(failingServerName, 50, dc, 0f);
    testServerManager.addServer("server150", 150, dc, 0f);
    testServerManager.addServer("server110", 110, dc, 0f);
    HttpClientFactory client = testServerManager.getClient(
        dc,
        new Profile()
    );
    AtomicInteger clientRequestsCounter = new AtomicInteger();
    AtomicInteger clientErrors = new AtomicInteger();
    executorService.invokeAll(IntStream.range(0, requests)
        .mapToObj(index -> (Callable<?>) () -> executeBalancingRequest(upstreamName, client, clientRequestsCounter, clientErrors))
        .collect(Collectors.toList()));
    assertEquals(0, clientErrors.get());

    testServerManager.serverFailRate.put(failingServerName, 1f);
    executorService.invokeAll(IntStream.range(0, requests)
        .mapToObj(index -> (Callable<?>) () -> executeBalancingRequest(upstreamName, client, clientRequestsCounter, clientErrors))
        .collect(Collectors.toList()));
    //TODO some fkn magic: connect timeouts on "healthy" servers if repeating all tasks. maybe macos???
    assertTrue("Client errors: " + clientErrors.get(), clientErrors.get() < requests / 1000);

    int sumWeight = testServerManager.servers.values().stream().mapToInt(Server::getWeight).sum();
    int allRequests = testServerManager.serverRequests.values().stream().mapToInt(LongAdder::intValue).sum();

    double handledRequestsPart = (double) testServerManager.serverRequests.get(failingServerName).intValue() / allRequests;
    double weightPart = (double) testServerManager.servers.get(failingServerName).getWeight() / sumWeight;
    assertTrue("Actual percent for failing server: " + handledRequestsPart * 100, handledRequestsPart <= weightPart);
    assertServerRequestDistribution(Set.of(failingServerName));
  }

  private Object executeBalancingRequest(
      String upstreamName,
      HttpClientFactory client,
      AtomicInteger clientRequestsCounter,
      AtomicInteger clientErrors
  ) {
    try {
      Request request = new RequestBuilder(JClientBase.HTTP_GET).setUrl("http://" + upstreamName).build();
      Response response = client.with(request).unconverted().get();
      if (HttpStatuses.OK != response.getStatusCode()) {
        clientErrors.incrementAndGet();
      }
      return null;
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      clientRequestsCounter.incrementAndGet();
    }
  }

  private void assertServerRequestDistribution(Set<String> excludedNames) {
    Map<String, LongAdder> filteredRequests = testServerManager.serverRequests.entrySet().stream()
        .filter(e -> !excludedNames.contains(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    int sumWeight = testServerManager.servers.entrySet().stream()
        .filter(e -> !excludedNames.contains(e.getKey()))
        .map(Map.Entry::getValue)
        .mapToInt(Server::getWeight)
        .sum();
    int allRequests = filteredRequests.values().stream().mapToInt(LongAdder::intValue).sum();
    List<Matcher<? super Map<String, LongAdder>>> matchers = testServerManager.servers.entrySet().stream()
        .filter(e -> !excludedNames.contains(e.getKey()))
        .map(nameToServer -> {
          double weightPart = (double) nameToServer.getValue().getWeight() / sumWeight;
          return new IsMapContaining<>(Matchers.equalTo(nameToServer.getKey()), new HandledRequestPartMatcher(weightPart, allRequests, 0.01));
        })
        .collect(Collectors.toList());
    MatcherAssert.assertThat(filteredRequests, Matchers.allOf(matchers));
  }

  private static final class TestServerManager {
    private static final Random RND = new Random();

    private final ServerStore serverStore;
    private final String upstreamName;

    private final Map<String, Server> servers;
    private final Map<String, Float> serverFailRate;
    private final int backlogSize;

    private HttpClientFactory client;
    private UpstreamManager upstreamManager;
    //stats
    private final Map<String, LongAdder> serverRequests;

    private TestServerManager(ServerStore serverStore, String upstreamName, int backlogSize) {
      this.serverStore = Objects.requireNonNull(serverStore);
      this.upstreamName = upstreamName;
      this.backlogSize = backlogSize;
      servers = new HashMap<>();
      serverFailRate = new HashMap<>();
      serverRequests = new ConcurrentHashMap<>();
    }

    public void addServer(String name, int weight, String dc, float serverFailRate) {
      if (servers.containsKey(name)) {
        throw new IllegalArgumentException("name=" + name + " already defined");
      }
      servers.put(name, new Server(createServer(name), weight, dc));
      this.serverFailRate.put(name, serverFailRate);
    }

    public void removeServer(String name) {
      Server serverToRemove = servers.remove(name);
      serverRequests.remove(name);
      serverStore.updateServers(upstreamName, List.of(), List.of(serverToRemove));
      upstreamManager.updateUpstreams(Set.of(upstreamName));
    }

    public HttpClientFactory getClient(String currentDc, Profile profile) {
      if (client == null) {
        if (servers.isEmpty()) {
          throw new IllegalStateException("add servers before getting client");
        }
        serverStore.updateServers(upstreamName, servers.values(), Set.of());
        var configStore = new ConfigStoreImpl();
        ApplicationConfig applicationConfig = new ApplicationConfig()
            .setHosts(Map.of(UpstreamConfig.DEFAULT, new Host().setProfiles(Map.of(UpstreamConfig.DEFAULT, profile))));
        configStore.updateConfig(upstreamName, ApplicationConfig.toUpstreamConfigs(applicationConfig, UpstreamConfig.DEFAULT));
        upstreamManager = buildBalancingUpstreamManager(currentDc, configStore);
        upstreamManager.updateUpstreams(Set.of(upstreamName));

        this.client = new HttpClientFactoryBuilder(new SingletonStorage<>(() -> new HttpClientContext(Map.of(), Map.of(), List.of())), List.of())
            .withConnectTimeoutMs(100)
            .withRequestStrategy(new BalancingRequestStrategy(upstreamManager, new TestUpstreamService(), new TestUpstreamConfigService()))
            .withCallbackExecutor(Runnable::run)
            .build();
      }
      return client;
    }

    private String createServer(String serverName) {
      Exchanger<Integer> portHolder = new Exchanger<>();
      var t = new Thread(() -> {
        try (ServerSocket ss = new ServerSocket(0, backlogSize)) {
          portHolder.exchange(ss.getLocalPort());
          while (true) {
            try (var socket = ss.accept();
                 var inputStream = socket.getInputStream();
                 var in = new BufferedReader(new InputStreamReader(inputStream));
                 var output = new PrintWriter(socket.getOutputStream())
            ) {
              float failRate = serverFailRate.getOrDefault(serverName, 0f);
              if (failRate > 0 && RND.nextFloat() < failRate) {
                return;
              }
              String arg;
              do {
                arg = in.readLine();
              } while (arg != null && !arg.isEmpty());
              output.println("HTTP/1.1 200 OK");
              output.println("");
            } finally {
              serverRequests.computeIfAbsent(serverName, ignored -> new LongAdder()).increment();
            }
          }
        } catch (RuntimeException | InterruptedException | IOException e) {
          if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }
          throw new RuntimeException(e);
        }
      }, serverName + "-thread");
      t.setDaemon(true);
      t.start();
      return tryGetAddress(portHolder);
    }

    private static JClientInfrastructureConfig buildInfraConfig(String serviceName, String dc) {
      return new JClientInfrastructureConfig() {
        @Override
        public String getServiceName() {
          return serviceName;
        }

        @Override
        public String getCurrentDC() {
          return dc;
        }

        @Override
        public String getCurrentNodeName() {
          return serviceName;
        }
      };
    }

    private BalancingUpstreamManager buildBalancingUpstreamManager(String currentDc, ConfigStoreImpl configStore) {
      return new BalancingUpstreamManager(
          configStore, serverStore,
          Set.of(), buildInfraConfig("test", currentDc),
          false
      );
    }

    private static String tryGetAddress(Exchanger<Integer> portHolder) {
      try {
        return "http://localhost:" + portHolder.exchange(0);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }
  }

  private static final class HandledRequestPartMatcher extends TypeSafeMatcher<LongAdder> {
    private final double expectedPart;
    private final int allHandledRequests;
    private final double delta;

    public HandledRequestPartMatcher(double expectedPart, int allHandledRequests, double delta) {
      this.expectedPart = expectedPart;
      this.allHandledRequests = allHandledRequests;
      this.delta = delta;
    }

    @Override
    protected boolean matchesSafely(LongAdder requests) {
      return Matchers.closeTo(expectedPart, delta).matches((double) requests.intValue() / allHandledRequests);
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("has handled ").appendValue(expectedPart * 100).appendText("% of all requests with delta ").appendValue(delta);
    }
  }
}
