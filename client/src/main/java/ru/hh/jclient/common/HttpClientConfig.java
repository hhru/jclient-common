package ru.hh.jclient.common;

import io.netty.handler.ssl.SslContext;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import ru.hh.jclient.common.metric.MetricConsumer;
import ru.hh.jclient.common.util.MDCCopy;
import ru.hh.jclient.common.util.storage.Storage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import static java.util.Optional.ofNullable;

//TODO rename to HttpClientFactoryBuilder
public final class HttpClientConfig {
  public static final double DEFAULT_TIMEOUT_MULTIPLIER = 1;

  private final DefaultAsyncHttpClientConfig.Builder configBuilder;

  private UpstreamManager upstreamManager = new DefaultUpstreamManager();
  private Executor callbackExecutor;
  private Set<String> hostsWithSession;
  private Storage<HttpClientContext> contextSupplier;
  private double timeoutMultiplier = DEFAULT_TIMEOUT_MULTIPLIER;

  private MetricConsumer metricConsumer;

  public static HttpClientConfig basedOn(Properties properties) {
    DefaultAsyncHttpClientConfig.Builder configBuilder = new DefaultAsyncHttpClientConfig.Builder();
    ofNullable(properties.getProperty(ConfigKeys.USER_AGENT)).ifPresent(configBuilder::setUserAgent);
    ofNullable(properties.getProperty(ConfigKeys.MAX_CONNECTIONS)).map(Integer::parseInt).ifPresent(configBuilder::setMaxConnections);
    ofNullable(properties.getProperty(ConfigKeys.MAX_REQUEST_RETRIES)).map(Integer::parseInt).ifPresent(configBuilder::setMaxRequestRetry);
    ofNullable(properties.getProperty(ConfigKeys.CONNECTION_TIMEOUT_MS)).map(Integer::parseInt).ifPresent(configBuilder::setConnectTimeout);
    ofNullable(properties.getProperty(ConfigKeys.READ_TIMEOUT_MS)).map(Integer::parseInt).ifPresent(configBuilder::setReadTimeout);
    ofNullable(properties.getProperty(ConfigKeys.REQUEST_TIMEOUT_MS)).map(Integer::parseInt).ifPresent(configBuilder::setRequestTimeout);
    ofNullable(properties.getProperty(ConfigKeys.FOLLOW_REDIRECT)).map(Boolean::parseBoolean).ifPresent(configBuilder::setFollowRedirect);
    ofNullable(properties.getProperty(ConfigKeys.COMPRESSION_ENFORCED)).map(Boolean::parseBoolean).ifPresent(configBuilder::setCompressionEnforced);
    ofNullable(properties.getProperty(ConfigKeys.ACCEPT_ANY_CERTIFICATE)).map(Boolean::parseBoolean)
        .ifPresent(configBuilder::setUseInsecureTrustManager);
    ofNullable(properties.getProperty(ConfigKeys.KEEP_ALIVE)).map(Boolean::parseBoolean).ifPresent(configBuilder::setKeepAlive);
    ofNullable(properties.getProperty(ConfigKeys.IO_THREADS_COUNT)).map(Integer::parseInt).ifPresent(configBuilder::setIoThreadsCount);

    HttpClientConfig httpClientConfig = new HttpClientConfig(configBuilder);
    ofNullable(properties.getProperty(ConfigKeys.TIMEOUT_MULTIPLIER)).map(Double::parseDouble).ifPresent(httpClientConfig::withTimeoutMultiplier);
    return httpClientConfig;
  }

  /**
   * use this only if there's not enough "with*" methods to cover all requirements
   * @param asyncClientConfig instance of {@link DefaultAsyncHttpClientConfig}
   * @return instance of HttpClientConfig based on passed config to continue building
   */
  public static HttpClientConfig basedOnNativeConfig(Object asyncClientConfig) {
    if (!(asyncClientConfig instanceof AsyncHttpClientConfig)) {
      throw new IllegalArgumentException("Argument must be of " + AsyncHttpClientConfig.class.getName());
    }
    return new HttpClientConfig(new DefaultAsyncHttpClientConfig.Builder((AsyncHttpClientConfig) asyncClientConfig));
  }

  private HttpClientConfig(DefaultAsyncHttpClientConfig.Builder configBuilder) {
    this.configBuilder = configBuilder;
  }

  public HttpClientConfig withUpstreamManager(UpstreamManager upstreamManager) {
    this.upstreamManager = upstreamManager;
    return this;
  }

  public HttpClientConfig withThreadFactory(ThreadFactory threadFactory) {
    this.configBuilder.setThreadFactory(threadFactory);
    return this;
  }

  public HttpClientConfig withCallbackExecutor(Executor callbackExecutor) {
    this.callbackExecutor = callbackExecutor;
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

  public HttpClientConfig withSSLContext(SslContext sslContext) {
    this.configBuilder.setSslContext(sslContext);
    return this;
  }

  public HttpClientConfig acceptAnyCetificate(boolean enabled) {
    this.configBuilder.setUseInsecureTrustManager(enabled);
    return this;
  }

  public HttpClientConfig withTimeoutMultiplier(double timeoutMultiplier) {
    this.timeoutMultiplier = timeoutMultiplier;
    return this;
  }

  public HttpClientBuilder build() {
    HttpClientBuilder httpClientBuilder = new HttpClientBuilder(
      buildClient(),
      hostsWithSession,
      contextSupplier,
      callbackExecutor,
      buildUpstreamManager()
    );
    ofNullable(metricConsumer).ifPresent(consumer -> consumer.accept(httpClientBuilder.getMetricProvider()));
    return httpClientBuilder;
  }

  private AsyncHttpClient buildClient() {
    AsyncHttpClientConfig clientConfig = applyTimeoutMultiplier(configBuilder).build();
    return MDCCopy.doWithoutContext(() -> new DefaultAsyncHttpClient(clientConfig));
  }

  private UpstreamManager buildUpstreamManager() {
    upstreamManager.setTimeoutMultiplier(timeoutMultiplier);
    return upstreamManager;
  }

  private DefaultAsyncHttpClientConfig.Builder applyTimeoutMultiplier(DefaultAsyncHttpClientConfig.Builder clientConfigBuilder) {
    AsyncHttpClientConfig config = clientConfigBuilder.build();
    DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder(config);
    builder.setConnectTimeout(config.getConnectTimeout() > 0 ? (int)(config.getConnectTimeout() * timeoutMultiplier) : config.getConnectTimeout());
    builder.setReadTimeout(config.getReadTimeout() > 0 ? (int)(config.getReadTimeout() * timeoutMultiplier) : config.getReadTimeout());
    builder.setRequestTimeout(config.getRequestTimeout() > 0 ? (int)(config.getRequestTimeout() * timeoutMultiplier) : config.getRequestTimeout());
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

  public static final class ConfigKeys {
    private ConfigKeys() {
    }

    public static final String USER_AGENT = "userAgent";

    public static final String MAX_CONNECTIONS = "maxTotalConnections";
    public static final String MAX_REQUEST_RETRIES = "maxRequestRetries";

    public static final String CONNECTION_TIMEOUT_MS = "connectionTimeoutMs";
    public static final String READ_TIMEOUT_MS = "readTimeoutMs";
    public static final String REQUEST_TIMEOUT_MS = "requestTimeoutMs";

    public static final String TIMEOUT_MULTIPLIER = "timeoutMultiplier";

    public static final String FOLLOW_REDIRECT = "followRedirect";
    public static final String COMPRESSION_ENFORCED = "compressionEnforced";

    public static final String ACCEPT_ANY_CERTIFICATE = "acceptAnyCertificate";
    public static final String KEEP_ALIVE = "keepAlive";
    public static final String IO_THREADS_COUNT = "ioThreadsCount";
  }
}
