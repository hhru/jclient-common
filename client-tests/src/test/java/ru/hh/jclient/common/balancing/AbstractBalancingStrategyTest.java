package ru.hh.jclient.common.balancing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.HttpClientFactoryBuilder;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces;
import ru.hh.jclient.common.util.storage.SingletonStorage;

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
import java.util.stream.Collectors;

public abstract class AbstractBalancingStrategyTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBalancingStrategyTest.class);

  protected static final String DATACENTER = "test";
  protected static final String TEST_UPSTREAM = "test-upstream";

  protected static HttpClientFactory buildBalancingFactory(String datacenterName,
                                                           String upstreamName,
                                                           Map<String, String> upstreamCfg,
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
    String upstreamCfgString = upstreamCfg.entrySet().stream()
        .map(entry -> String.join("=", entry.getKey(), entry.getValue()))
        .collect(Collectors.joining(" "));
    String serverCfgString = adressesByWeight.entrySet().stream()
        .flatMap(weightToAddresses -> weightToAddresses.getValue().stream()
            .map(address -> String.join(" ", "server=" + address, "weight=" + weightToAddresses.getKey(), "dc=" + datacenterName))
        ).collect(Collectors.joining(" | "));
    BalancingUpstreamManager upstreamManager = new BalancingUpstreamManager(
        Map.of(upstreamName, String.join(" | ", upstreamCfgString, serverCfgString)),
        scheduledExecutorService,
        Set.of(tracking), datacenterName, false
    );
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
