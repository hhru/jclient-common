package ru.hh.jclient.common.bench;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.UnaryOperator;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.Uri;
import ru.hh.jclient.common.balancing.BalancingUpstreamManager;
import ru.hh.jclient.common.balancing.ConfigStore;
import ru.hh.jclient.common.balancing.ConfigStoreImpl;
import ru.hh.jclient.common.balancing.JClientInfrastructureConfig;
import ru.hh.jclient.common.balancing.MockBalancerBuilder;
import ru.hh.jclient.common.balancing.RequestBalancerBuilder;
import ru.hh.jclient.common.balancing.Server;
import ru.hh.jclient.common.balancing.ServerStore;
import ru.hh.jclient.common.balancing.ServerStoreImpl;
import ru.hh.jclient.common.balancing.UpstreamConfig;
import ru.hh.jclient.common.balancing.UpstreamManager;
import ru.hh.jclient.common.balancing.config.ApplicationConfig;
import ru.hh.jclient.common.util.storage.SingletonStorage;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
public class BalancingPerformanceBenchmark {
  private static final List<Server> servers = List.of(
      new Server("server1", 50, "test"),
      new Server("server2", 100, "test"),
      new Server("server3", 200, "test"),
      new Server("server4", 50, "test"),
      new Server("server1", 100, "test"),
      new Server("server2", 200, "test"),
      new Server("server3", 50, "test"),
      new Server("server4", 200, "test")
  );
  private final ServerStore serverStore = new ServerStoreImpl();
  private final ConfigStore configStore = new ConfigStoreImpl();
  private final JClientInfrastructureConfig infrastructureConfig = new JClientInfrastructureConfig() {
    @Override
    public String getServiceName() {
      return "test";
    }

    @Override
    public String getCurrentDC() {
      return "test";
    }

    @Override
    public String getCurrentNodeName() {
      return "test";
    }
  };
  private final UpstreamManager manager = new BalancingUpstreamManager(
      configStore,
      serverStore,
      Set.of(),
      infrastructureConfig,
      false
  );
  private final AsyncHttpClient httpClient = new DefaultAsyncHttpClient();
  private HttpClientFactory factory;

  public static void main(String[] args) throws RunnerException {
    var opt = new OptionsBuilder()
        .include(BalancingPerformanceBenchmark.class.getSimpleName())
        .forks(1)
        .jvmArgsAppend("-DrootLoggingLevel=WARN")
        .build();
    new Runner(opt).run();
  }

  @Setup
  public void setUp() {
    serverStore.updateServers("test", servers, Set.of());
    configStore.updateConfig("test", ApplicationConfig.toUpstreamConfigs(new ApplicationConfig(), UpstreamConfig.DEFAULT));
    manager.updateUpstreams(Set.of("test"));
    factory = new HttpClientFactory(
        httpClient,
        new SingletonStorage<>(() -> new HttpClientContext(Map.of(), Map.of(), List.of())),
        Set.of(),
        Runnable::run,
        new CustomStrategy(manager, UnaryOperator.identity())
    );
  }

  @TearDown
  public void tearDown() throws IOException {
    httpClient.close();
  }

  @Benchmark
  public void singleThread(Blackhole bh) throws ExecutionException, InterruptedException {
    bh.consume(factory.with(buildRequest().build()).unconverted().get());
  }

  @Threads(16)
  @Benchmark
  public void sixteenThreads(Blackhole bh) throws ExecutionException, InterruptedException {
    bh.consume(factory.with(buildRequest().build()).unconverted().get());
  }

  @Threads(32)
  @Benchmark
  public void thirtyTwoThreads(Blackhole bh) throws ExecutionException, InterruptedException {
    bh.consume(factory.with(buildRequest().build()).unconverted().get());
  }

  @Threads(96)
  @Benchmark
  public void nintySixThreads(Blackhole bh) throws ExecutionException, InterruptedException {
    bh.consume(factory.with(buildRequest().build()).unconverted().get());
  }

  public static RequestBuilder buildRequest() {
    return new RequestBuilder("GET").setUrl("http://test/status");
  }

  private static final class CustomStrategy implements RequestStrategy<RequestBalancerBuilder> {

    private final UpstreamManager upstreamManager;
    private final UnaryOperator<RequestBalancerBuilder> configAction;

    private CustomStrategy(UpstreamManager upstreamManager, UnaryOperator<RequestBalancerBuilder> configAction) {
      this.upstreamManager = upstreamManager;
      this.configAction = configAction;
    }

    @Override
    public RequestBalancerBuilder createRequestEngineBuilder(HttpClient client) {
      return configAction.apply(new MockBalancerBuilder(upstreamManager, client, new ResponseMock()));
    }

    @Override
    public RequestStrategy<RequestBalancerBuilder> createCustomizedCopy(UnaryOperator<RequestBalancerBuilder> configAction) {
      return new CustomStrategy(upstreamManager, this.configAction.andThen(configAction)::apply);
    }
  }

  private static final class ResponseMock extends Response {
    @Override
    public Uri getUri() {
      return Uri.create("http://localhost:80");
    }

    @Override
    public int getStatusCode() {
      return 200;
    }

    @Override
    public String getStatusText() {
      return "OK";
    }
  }
}
