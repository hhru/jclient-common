package ru.hh.jclient.common.balancing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.HttpClientFactoryBuilder;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces;
import ru.hh.jclient.common.util.storage.SingletonStorage;
import ru.hh.jclient.consul.UpstreamConfigService;
import ru.hh.jclient.consul.UpstreamService;
import ru.hh.jclient.consul.model.ApplicationConfig;
import ru.hh.jclient.consul.model.Host;
import ru.hh.jclient.consul.model.Profile;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Exchanger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class AbstractBalancingStrategyTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBalancingStrategyTest.class);
  private static final Profile EMPTY_PROFILE = new Profile();
  protected static final String DATACENTER = "test";
  protected static final String TEST_UPSTREAM = "test-upstream";

  protected static HttpClientFactory buildBalancingFactory(String datacenterName,
                                                           String upstreamName,
                                                           Map<Integer, List<String>> adressesByWeight, ConcurrentMap<String,
                                                           List<Integer>> trackingHolder) {
    return buildBalancingFactory(datacenterName, upstreamName, EMPTY_PROFILE, adressesByWeight, trackingHolder);

  }

  protected static HttpClientFactory buildBalancingFactory(String datacenterName,
                                                           String upstreamName,
                                                           Profile profile,
                                                           Map<Integer, List<String>> adressesByWeight, ConcurrentMap<String,
                                                           List<Integer>> trackingHolder) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    var tracking = new Monitoring() {
      @Override
      public void countRequest(String upstreamName, String serverDatacenter, String serverAddress, int statusCode,
                               long requestTimeMicros, boolean isRequestFinal) {
        trackingHolder.computeIfAbsent(serverAddress, addr -> new CopyOnWriteArrayList<>()).add(statusCode);
      }

      @Override
      public void countRequestTime(String upstreamName, String serverDatacenter, long requestTimeMicros) {}

      @Override
      public void countRetry(String upstreamName, String serverDatacenter, String serverAddress, int statusCode,
                             int firstStatusCode, int retryCount) {}
    };
    BalancingUpstreamManager upstreamManager = new BalancingUpstreamManager(scheduledExecutorService, Set.of(tracking), datacenterName, false,
      new UpstreamConfigService() {
        @Override
        public ApplicationConfig getUpstreamConfig(String application) {
          return new ApplicationConfig().setHosts(Map.of("default", new Host().setProfiles(Map.of("default", profile))));
        }

        @Override
        public void setupListener(Consumer<String> callback) {
        }
    }, new UpstreamService() {
         @Override
         public void setupListener(Consumer<String> callback) {}

         @Override
         public List<Server> getServers(String serviceName) {
           return adressesByWeight.entrySet().stream()
             .flatMap(entry -> entry.getValue().stream().map(address -> new Server(address, entry.getKey(), datacenterName)))
             .collect(Collectors.toList());
         }
    });
    upstreamManager.updateUpstream(upstreamName);
    var strategy = new BalancingRequestStrategy(upstreamManager);
    var contextSupplier = new SingletonStorage<>(() -> new HttpClientContext(Map.of(), Map.of(), List.of()));
    return new HttpClientFactoryBuilder(contextSupplier, List.of())
      .withConnectTimeoutMs(100)
      .withRequestStrategy(strategy)
      .withCallbackExecutor(Runnable::run)
      .build();
  }

  protected static String createNormallyWorkingServer() {
    return createServer(sock -> {
      try (Socket socket = sock;
           var inputStream = socket.getInputStream();
           var output = new PrintWriter(socket.getOutputStream())
      ) {
        long start = System.currentTimeMillis();
        LOGGER.trace(new String(startRead(inputStream), Charset.defaultCharset()));
        output.println("HTTP/1.1 200 OK");
        output.println("");
        output.flush();
        LOGGER.debug("Handled request in {}ms", System.currentTimeMillis() - start);
      }
    });
  }

  protected static String createServer(MoreFunctionalInterfaces.FailableConsumer<Socket, Exception> handler) {
    Exchanger<Integer> portHolder = new Exchanger<>();
    var t = new Thread(() -> {
      try (ServerSocket ss = new ServerSocket(0)) {
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
}