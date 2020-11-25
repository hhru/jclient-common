package ru.hh.jclient.common.balancing;

import static java.util.Collections.singleton;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpClientFactory;
import ru.hh.jclient.common.Monitoring;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.RequestContext;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.jclient.common.util.storage.SingletonStorage;
import ru.hh.jclient.consul.UpstreamService;
import ru.hh.jclient.consul.model.ApplicationConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
public class CustomizationBenchmark {
  private static final String UPSTREAM = "up1";
  private static final List<Server> servers = List.of(new Server("server1", 1, null),
          new Server("server2", 1, null));
  private static final CustomUpstreamService CUSTOM_UPSTREAM_SERVICE = new CustomUpstreamService();

  public static void main(String[] args) throws RunnerException {
    var opt = new OptionsBuilder()
        .include(CustomizationBenchmark.class.getSimpleName())
        .forks(1)
        .build();
    new Runner(opt).run();
  }

  private static final Upstream upstream = new Upstream(UPSTREAM,
      UpstreamConfig.fromApplicationConfig(new ApplicationConfig(), null, null), servers, Executors.newScheduledThreadPool(1)
  );
  private static final UpstreamManager manager = new UpstreamManager() {

    @Override
    public List<Server> getServersForService(String upstreamName) {
      return servers;
    }

    @Override
    public void updateUpstream(@Nonnull String upstreamName) {

    }

    @Override
    public Upstream getUpstream(String serviceName, @Nullable String profile) {
      return upstream;
    }

    @Override
    Map<String, UpstreamGroup> getUpstreams() {
      return Map.of(upstream.getName(), new UpstreamGroup(UPSTREAM, "test", upstream));
    }

    @Override
    public Set<Monitoring> getMonitoring() {
      return Set.of();
    }
  };
  private static final AsyncHttpClient httpClient = new DefaultAsyncHttpClient();
  private HttpClientFactory factory;

  @Setup
  public void setUp() {


      factory = new HttpClientFactory(httpClient, singleton("http://localhost"),
          new SingletonStorage<>(() -> new HttpClientContext(Map.of(), Map.of(), List.of())),
          Runnable::run,
          new CustomStrategy(manager, UnaryOperator.identity())
      );
  }

  @TearDown
  public void tearDown() throws IOException {
    httpClient.close();
  }



  @Benchmark
  public void base(Blackhole bh) {
    bh.consume(factory.with(buildRequest().build()).expectNoContent().result());
  }

  @Benchmark
  public void builderCustomization(Blackhole bh) {
    bh.consume(factory.with(buildRequest().build()).configureRequestEngine(RequestBalancerBuilder.class)
        .makeAdaptive()
        .backToClient()
        .expectNoContent().result());
  }

  @Benchmark
  public void copyFactoryCustomization(Blackhole bh) {
    bh.consume(factory.createCustomizedCopy((UnaryOperator<RequestBalancerBuilder>) RequestBalancerBuilder::makeAdaptive)
        .with(buildRequest().build())
        .expectNoContent().result());
  }

  public static RequestBuilder buildRequest() {
    return new RequestBuilder("GET").setUrl("http://localhost/status");
  }

  private static final class CustomBuilder extends RequestBalancerBuilder {

    CustomBuilder(UpstreamManager upstreamManager, HttpClient client) {
      super(upstreamManager, client);
    }

    @Override
    public RequestBalancer build(Request request, RequestStrategy.RequestExecutor requestExecutor) {
      return super.build(request, new RequestStrategy.RequestExecutor() {
        @Override
        public CompletableFuture<ResponseWrapper> executeRequest(Request request, int retryCount, RequestContext context) {
          return CompletableFuture.completedFuture(new ResponseWrapper(null, 1));
        }

        @Override
        public int getDefaultRequestTimeoutMs() {
          return httpClient.getConfig().getRequestTimeout();
        }
      });
    }
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
      return configAction.apply(new CustomBuilder(upstreamManager, client));
    }

    @Override
    public void setTimeoutMultiplier(double timeoutMultiplier) {

    }

    @Override
    public RequestStrategy<RequestBalancerBuilder> createCustomizedCopy(UnaryOperator<RequestBalancerBuilder> configAction) {
      return new CustomStrategy(upstreamManager, configAction);
    }
  }
  private static final class CustomUpstreamService implements UpstreamService{

    @Override
    public void setupListener(Consumer<String> callback) {

    }

    @Override
    public List<Server> getServers(String serviceName) {
      return servers;
    }
  }
}
