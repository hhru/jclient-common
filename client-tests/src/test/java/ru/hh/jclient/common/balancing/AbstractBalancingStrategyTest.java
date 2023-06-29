package ru.hh.jclient.common.balancing;

import jakarta.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.HttpClientFactoryBuilder;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.balancing.config.ApplicationConfig;
import ru.hh.jclient.common.balancing.config.Host;
import ru.hh.jclient.common.balancing.config.Profile;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces;
import ru.hh.jclient.common.util.storage.SingletonStorage;
import ru.hh.jclient.consul.TestUpstreamConfigService;
import ru.hh.jclient.consul.TestUpstreamService;

public abstract class AbstractBalancingStrategyTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBalancingStrategyTest.class);
  private static final Profile EMPTY_PROFILE = new Profile();
  protected static final String DATACENTER = "Test";
  protected static final String TEST_UPSTREAM = "test-upstream";
  protected static final JClientInfrastructureConfig infrastructureConfig = new JClientInfrastructureConfig() {
    @Override
    public String getServiceName() {
      return null;
    }

    @Override
    public String getCurrentDC() {
      return DATACENTER;
    }

    @Override
    public String getCurrentNodeName() {
      return null;
    }
  };

  protected static Map.Entry<HttpClientFactory, UpstreamManager> buildBalancingFactory(
      String upstreamName,
      ServerStore serverStore,
      ConcurrentMap<String, List<Integer>> trackingHolder
  ) {
    return buildBalancingFactory(upstreamName, EMPTY_PROFILE, serverStore, trackingHolder, null);
  }

  protected static Map.Entry<HttpClientFactory, UpstreamManager> buildBalancingFactory(
      String upstreamName,
      Profile profile,
      ServerStore serverStore,
      ConcurrentMap<String, List<Integer>> trackingHolder,
      @Nullable ConcurrentMap<String, LongAdder> retries
  ) {
    var tracking = new Monitoring() {
      @Override
      public void countRequest(
          String upstreamName,
          String serverDatacenter,
          String serverAddress,
          int statusCode,
          long requestTimeMillis,
          boolean isRequestFinal
      ) {
        trackingHolder.computeIfAbsent(serverAddress, addr -> new CopyOnWriteArrayList<>()).add(statusCode);
      }

      @Override
      public void countRequestTime(String upstreamName, String serverDatacenter, long requestTimeMillis) {}

      @Override
      public void countRetry(
          String upstreamName,
          String serverDatacenter,
          String serverAddress,
          int statusCode,
          int firstStatusCode,
          int triesUsed
      ) {
        Optional.ofNullable(retries).ifPresent(map -> map.computeIfAbsent(serverAddress, ignored -> new LongAdder()).increment());
      }

      @Override
      public void countUpdateIgnore(String upstreamName, String serverDatacenter) {}
    };
    var configStore = new ConfigStoreImpl();
    ApplicationConfig applicationConfig = new ApplicationConfig()
        .setHosts(Map.of(UpstreamConfig.DEFAULT, new Host().setProfiles(Map.of(UpstreamConfig.DEFAULT, profile))));
    configStore.updateConfig(upstreamName, ApplicationConfig.toUpstreamConfigs(applicationConfig, UpstreamConfig.DEFAULT));
    BalancingUpstreamManager upstreamManager = new BalancingUpstreamManager(
        configStore,
        serverStore,
        Set.of(tracking),
        infrastructureConfig,
        false
    );
    upstreamManager.updateUpstreams(Set.of(upstreamName));
    var strategy = new BalancingRequestStrategy(upstreamManager, new TestUpstreamService(), new TestUpstreamConfigService());
    var contextSupplier = new SingletonStorage<>(() -> new HttpClientContext(Map.of(), Map.of(), List.of()));
    return Map.entry(
        new HttpClientFactoryBuilder(contextSupplier, List.of())
            .withConnectTimeoutMs(100)
            .withRequestStrategy(strategy)
            .withCallbackExecutor(Runnable::run)
            .build(),
        upstreamManager
    );
  }

  protected static String createNormallyWorkingServer() {
    return createServer(socket -> {
      try (socket;
           var inputStream = socket.getInputStream();
           var in = new BufferedReader(new InputStreamReader(inputStream));
           var output = new PrintWriter(socket.getOutputStream())
      ) {
        long start = System.currentTimeMillis();
        StringBuilder request = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
          request.append(line);
        }
        LOGGER.trace(request.toString());
        Thread.sleep(5);
        output.println("HTTP/1.1 200 OK");
        output.println("");
        output.flush();
        LOGGER.debug("Handled request in {}ms", System.currentTimeMillis() - start);
      }
    });
  }

  protected static String createServer(MoreFunctionalInterfaces.FailableConsumer<Socket, Exception> handler) {
    return createServer(handler, 50);
  }

  protected static String createServer(MoreFunctionalInterfaces.FailableConsumer<Socket, Exception> handler, int backlog) {
    Exchanger<Integer> portHolder = new Exchanger<>();
    var t = new Thread(() -> {
      try (ServerSocket ss = new ServerSocket(0, backlog)) {
        portHolder.exchange(ss.getLocalPort());
        while (true) {
          handler.accept(ss.accept());
        }
      } catch (Exception e) {
        if (e instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
        throw new RuntimeException(e);
      }
    });
    t.setDaemon(true);
    t.start();
    return tryGetAddress(portHolder);
  }

  protected static String tryGetAddress(Exchanger<Integer> portHolder) {
    try {
      return "http://localhost:" + portHolder.exchange(0);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  protected static byte[] startRead(InputStream inputStream) throws IOException {
    byte[] buffer = new byte[4];
    int length = inputStream.read(buffer);
    return Arrays.copyOf(buffer, length);
  }

  protected static class TestStoreFromAddress implements ServerStore {
    private final String datacenterName;
    private final Map<Integer, List<String>> adressesByWeight;

    public TestStoreFromAddress(String datacenterName, Map<Integer, List<String>> adressesByWeight) {
      this.datacenterName = datacenterName;
      this.adressesByWeight = adressesByWeight;
    }

    @Override
    public List<Server> getServers(String serviceName) {
      return adressesByWeight
          .entrySet()
          .stream()
          .flatMap(entry -> entry.getValue().stream().map(address -> new Server(address, entry.getKey(), datacenterName)))
          .collect(Collectors.toList());
    }

    @Override
    public Optional<Integer> getInitialSize(String serviceName) {
      return Optional.of(adressesByWeight.values().stream().mapToInt(List::size).sum());
    }

    @Override
    public void updateServers(String serviceName, Collection<Server> aliveServers, Collection<Server> deadServers) {

    }
  }
}
