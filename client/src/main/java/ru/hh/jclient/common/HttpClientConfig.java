package ru.hh.jclient.common;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpProviderConfig;
import com.ning.http.client.filter.RequestFilter;
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;
import ru.hh.jclient.common.metric.MetricConsumer;
import ru.hh.jclient.common.util.MDCCopy;
import ru.hh.jclient.common.util.stats.SlowRequestsLoggingHandler;
import ru.hh.jclient.common.util.storage.Storage;

import javax.net.ssl.SSLContext;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

//TODO rename to HttpClientFactoryBuilder
public final class HttpClientConfig {

  private final AsyncHttpClientConfig.Builder configBuilder;

  private UpstreamManager upstreamManager;
  private Executor callbackExecutor;
  private NettyAsyncHttpProviderConfig nettyConfig;
  private Set<String> hostsWithSession;
  private Storage<HttpClientContext> contextSupplier;
  private double timeoutMultiplier = 1;

  private MetricConsumer metricConsumer;

  public static HttpClientConfig basedOn(Properties properties) {
    AsyncHttpClientConfig.Builder configBuilder = new AsyncHttpClientConfig.Builder();
    ofNullable(properties.getProperty(ConfigKeys.USER_AGENT)).ifPresent(configBuilder::setUserAgent);
    ofNullable(properties.getProperty(ConfigKeys.MAX_CONNECTIONS)).map(Integer::parseInt).ifPresent(configBuilder::setMaxConnections);
    ofNullable(properties.getProperty(ConfigKeys.MAX_REQUEST_RETRIES)).map(Integer::parseInt).ifPresent(configBuilder::setMaxRequestRetry);
    ofNullable(properties.getProperty(ConfigKeys.CONNECTION_TIMEOUT_MS)).map(Integer::parseInt).ifPresent(configBuilder::setConnectTimeout);
    ofNullable(properties.getProperty(ConfigKeys.READ_TIMEOUT_MS)).map(Integer::parseInt).ifPresent(configBuilder::setReadTimeout);
    ofNullable(properties.getProperty(ConfigKeys.REQUEST_TIMEOUT_MS)).map(Integer::parseInt).ifPresent(configBuilder::setRequestTimeout);
    ofNullable(properties.getProperty(ConfigKeys.FOLLOW_REDIRECT)).map(Boolean::parseBoolean).ifPresent(configBuilder::setFollowRedirect);
    ofNullable(properties.getProperty(ConfigKeys.COMPRESSION_ENFORCED)).map(Boolean::parseBoolean).ifPresent(configBuilder::setCompressionEnforced);
    ofNullable(properties.getProperty(ConfigKeys.ALLOW_POOLING_CONNECTIONS)).map(Boolean::parseBoolean)
        .ifPresent(configBuilder::setAllowPoolingConnections);
    HttpClientConfig httpClientConfig = new HttpClientConfig(configBuilder);
    ofNullable(properties.getProperty(ConfigKeys.TIMEOUT_MULTIPLIER)).map(Double::parseDouble).ifPresent(httpClientConfig::withTimeoutMultiplier);
    httpClientConfig.nettyConfig = new NettyAsyncHttpProviderConfig();
    //to be able to monitor netty boss thread pool. See: com.ning.http.client.providers.netty.channel.ChannelManager
    httpClientConfig.nettyConfig.setBossExecutorService(Executors.newCachedThreadPool());
    configBuilder.setAsyncHttpClientProviderConfig(httpClientConfig.nettyConfig);
    int slowRequestThreshold = ofNullable(properties.getProperty(ConfigKeys.SLOW_REQ_THRESHOLD_MS)).map(Integer::parseInt).orElse(2000);
    NettyAsyncHttpProviderConfig.AdditionalPipelineInitializer initializer = pipeline -> {
      pipeline.addFirst("slowRequestLogger", new SlowRequestsLoggingHandler(slowRequestThreshold, TimeUnit.MILLISECONDS));
    };
    httpClientConfig.nettyConfig.setHttpAdditionalPipelineInitializer(initializer);
    return httpClientConfig;
  }

  /**
   * use this only if there's not enough "with*" methods to cover all requirements
   * example: you need to set {@link AsyncHttpProviderConfig}
   * example: you need to set {@link RequestFilter}
   * @param asyncClientConfig instance of {@link AsyncHttpClientConfig}
   * @return instance of HttpClientConfig based on passed config to continue building
   */
  public static HttpClientConfig basedOnNativeConfig(Object asyncClientConfig) {
    if (!(asyncClientConfig instanceof AsyncHttpClientConfig)) {
      throw new IllegalArgumentException("Argument must be of " + AsyncHttpClientConfig.class.getName());
    }
    return new HttpClientConfig(new AsyncHttpClientConfig.Builder((AsyncHttpClientConfig) asyncClientConfig));
  }

  private HttpClientConfig(AsyncHttpClientConfig.Builder configBuilder) {
    this.configBuilder = configBuilder;
  }

  public HttpClientConfig withUpstreamManager(UpstreamManager upstreamManager) {
    this.upstreamManager = upstreamManager;
    return this;
  }

  public HttpClientConfig withExecutorService(ExecutorService applicationThreadPool) {
    this.configBuilder.setExecutorService(applicationThreadPool);
    return this;
  }

  public HttpClientConfig withCallbackExecutor(Executor callbackExecutor) {
    this.callbackExecutor = callbackExecutor;
    return this;
  }

  public HttpClientConfig withBossNettyExecutor(ExecutorService nettyBossExecutorService) {
    this.nettyConfig.setBossExecutorService(nettyBossExecutorService);
    return this;
  }

  public HttpClientConfig withHostsWithSession(Collection<String> hostsWithSession) {
    this.hostsWithSession = new HashSet<>(hostsWithSession);
    return this;
  }

  public HttpClientConfig withStorage(Storage<HttpClientContext> contextSupplier) {
    this.contextSupplier = contextSupplier;
    return this;
  }

  public HttpClientConfig withMetricConsumer(MetricConsumer metricConsumer) {
    this.metricConsumer = metricConsumer;
    return this;
  }

  public HttpClientConfig withSSLContext(SSLContext sslContext) {
    this.configBuilder.setSSLContext(sslContext);
    return this;
  }

  public HttpClientConfig withTimeoutMultiplier(double timeoutMultiplier) {
    this.timeoutMultiplier = timeoutMultiplier;
    return this;
  }

  public HttpClientBuilder build() {
    AsyncHttpClient http = buildClient();
    HttpClientBuilder httpClientBuilder = new HttpClientBuilder(
      http,
      hostsWithSession,
      contextSupplier,
      callbackExecutor,
      upstreamManager
    );
    ofNullable(metricConsumer).ifPresent(consumer -> consumer.accept(httpClientBuilder.getMetricProvider()));
    return httpClientBuilder;
  }

  private AsyncHttpClient buildClient() {
    AsyncHttpClientConfig clientConfig = applyTimeoutMultiplier(configBuilder).build();
    return MDCCopy.doWithoutContext(() -> new AsyncHttpClient(new NettyAsyncHttpProvider(clientConfig), clientConfig));
  }

  private AsyncHttpClientConfig.Builder applyTimeoutMultiplier(AsyncHttpClientConfig.Builder clientConfigBuilder) {
    AsyncHttpClientConfig config = clientConfigBuilder.build();
    AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder(config);
    builder.setConnectTimeout((int)(config.getConnectTimeout() * timeoutMultiplier));
    builder.setReadTimeout((int)(config.getReadTimeout() * timeoutMultiplier));
    builder.setRequestTimeout((int)(config.getRequestTimeout() * timeoutMultiplier));
    return builder;
  }

  public HttpClientConfig withUserAgent(String userAgent) {
    configBuilder.setUserAgent(userAgent);
    return this;
  }

  public HttpClientConfig withMaxConnections(int maxConnections) {
    configBuilder.setMaxConnections(maxConnections);
    return this;
  }

  public HttpClientConfig withMaxRequestRetries(int maxRequestRetries) {
    configBuilder.setMaxRequestRetry(maxRequestRetries);
    return this;
  }

  public HttpClientConfig withConnectTimeoutMs(int connectTimeoutMs) {
    configBuilder.setConnectTimeout(connectTimeoutMs);
    return this;
  }

  public HttpClientConfig withReadTimeoutMs(int readTimeoutMs) {
    configBuilder.setReadTimeout(readTimeoutMs);
    return this;
  }

  public HttpClientConfig withRequestTimeoutMs(int requestTimeoutMs) {
    configBuilder.setRequestTimeout(requestTimeoutMs);
    return this;
  }

  public interface ConfigKeys {
    String SLOW_REQ_THRESHOLD_MS = "slowRequestThresholdMs";
    String USER_AGENT = "userAgent";

    String MAX_CONNECTIONS = "maxTotalConnections";
    String MAX_REQUEST_RETRIES = "maxRequestRetries";

    String CONNECTION_TIMEOUT_MS = "connectionTimeoutMs";
    String READ_TIMEOUT_MS = "readTimeoutMs";
    String REQUEST_TIMEOUT_MS = "requestTimeoutMs";

    String TIMEOUT_MULTIPLIER = "timeoutMultiplier";

    String FOLLOW_REDIRECT = "followRedirect";
    String COMPRESSION_ENFORCED = "compressionEnforced";
    String ALLOW_POOLING_CONNECTIONS = "allowPoolingConnections";
  }
}
