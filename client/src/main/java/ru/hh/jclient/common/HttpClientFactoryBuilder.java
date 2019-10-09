package ru.hh.jclient.common;

import io.netty.handler.ssl.SslContext;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import ru.hh.jclient.common.metrics.MetricsConsumer;
import ru.hh.jclient.common.util.MDCCopy;
import ru.hh.jclient.common.util.storage.Storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import static java.util.Optional.ofNullable;

public final class HttpClientFactoryBuilder {
  public static final double DEFAULT_TIMEOUT_MULTIPLIER = 1;

  private DefaultAsyncHttpClientConfig.Builder configBuilder;
  private UpstreamManager upstreamManager = new DefaultUpstreamManager();
  private Executor callbackExecutor;
  private Set<String> hostsWithSession;
  private Storage<HttpClientContext> contextSupplier;
  private double timeoutMultiplier = DEFAULT_TIMEOUT_MULTIPLIER;
  private MetricsConsumer metricsConsumer;
  private List<HttpClientEventListener> eventListeners = new ArrayList<>();

  public HttpClientFactoryBuilder(Storage<HttpClientContext> contextSupplier, List<HttpClientEventListener> eventListeners) {
    this.configBuilder = new DefaultAsyncHttpClientConfig.Builder();
    this.contextSupplier = contextSupplier;
    this.eventListeners = new ArrayList<>(eventListeners);
  }

  public HttpClientFactoryBuilder withProperties(Properties properties) {
    ofNullable(properties.getProperty(ConfigKeys.MAX_CONNECTIONS)).map(Integer::parseInt)
      .ifPresent(this::withMaxConnections);
    ofNullable(properties.getProperty(ConfigKeys.MAX_REQUEST_RETRIES)).map(Integer::parseInt)
      .ifPresent(this::withMaxRequestRetries);
    ofNullable(properties.getProperty(ConfigKeys.CONNECTION_TIMEOUT_MS)).map(Integer::parseInt)
      .ifPresent(this::withConnectTimeoutMs);
    ofNullable(properties.getProperty(ConfigKeys.READ_TIMEOUT_MS)).map(Integer::parseInt)
      .ifPresent(this::withReadTimeoutMs);
    ofNullable(properties.getProperty(ConfigKeys.REQUEST_TIMEOUT_MS)).map(Integer::parseInt)
      .ifPresent(this::withRequestTimeoutMs);
    ofNullable(properties.getProperty(ConfigKeys.TIMEOUT_MULTIPLIER)).map(Double::parseDouble)
      .ifPresent(this::withTimeoutMultiplier);
    ofNullable(properties.getProperty(ConfigKeys.USER_AGENT))
      .ifPresent(this::withUserAgent);

    ofNullable(properties.getProperty(ConfigKeys.FOLLOW_REDIRECT)).map(Boolean::parseBoolean)
      .ifPresent(configBuilder::setFollowRedirect);
    ofNullable(properties.getProperty(ConfigKeys.COMPRESSION_ENFORCED)).map(Boolean::parseBoolean)
      .ifPresent(configBuilder::setCompressionEnforced);
    ofNullable(properties.getProperty(ConfigKeys.ACCEPT_ANY_CERTIFICATE)).map(Boolean::parseBoolean)
      .ifPresent(configBuilder::setUseInsecureTrustManager);
    ofNullable(properties.getProperty(ConfigKeys.KEEP_ALIVE)).map(Boolean::parseBoolean)
      .ifPresent(configBuilder::setKeepAlive);
    ofNullable(properties.getProperty(ConfigKeys.IO_THREADS_COUNT)).map(Integer::parseInt)
      .ifPresent(configBuilder::setIoThreadsCount);

    return this;
  }

  /**
   * use this only if there's not enough "with*" methods to cover all requirements
   * @param asyncClientConfig instance of {@link DefaultAsyncHttpClientConfig}
   * @return instance of HttpClientFactoryBuilder based on passed config to continue building
   */
  public HttpClientFactoryBuilder withNativeConfig(Object asyncClientConfig) {
    if (!(asyncClientConfig instanceof AsyncHttpClientConfig)) {
      throw new IllegalArgumentException("Argument must be of " + AsyncHttpClientConfig.class.getName());
    }
    this.configBuilder = new DefaultAsyncHttpClientConfig.Builder((AsyncHttpClientConfig) asyncClientConfig);
    return this;
  }

  public HttpClientFactoryBuilder addEventListener(HttpClientEventListener eventListener) {
    this.eventListeners.add(eventListener);
    return this;
  }

  public HttpClientFactoryBuilder withUpstreamManager(UpstreamManager upstreamManager) {
    this.upstreamManager = upstreamManager;
    return this;
  }

  public HttpClientFactoryBuilder withThreadFactory(ThreadFactory threadFactory) {
    this.configBuilder.setThreadFactory(threadFactory);
    return this;
  }

  public HttpClientFactoryBuilder withCallbackExecutor(Executor callbackExecutor) {
    this.callbackExecutor = callbackExecutor;
    return this;
  }

  public HttpClientFactoryBuilder withHostsWithSession(Collection<String> hostsWithSession) {
    this.hostsWithSession = new HashSet<>(hostsWithSession);
    return this;
  }

  public HttpClientFactoryBuilder withStorage(Storage<HttpClientContext> contextSupplier) {
    this.contextSupplier = contextSupplier;
    return this;
  }

  public HttpClientFactoryBuilder withMetricsConsumer(MetricsConsumer metricsConsumer) {
    this.metricsConsumer = metricsConsumer;
    return this;
  }

  public HttpClientFactoryBuilder withSSLContext(SslContext sslContext) {
    this.configBuilder.setSslContext(sslContext);
    return this;
  }

  public HttpClientFactoryBuilder acceptAnyCertificate(boolean enabled) {
    this.configBuilder.setUseInsecureTrustManager(enabled);
    return this;
  }

  public HttpClientFactoryBuilder withTimeoutMultiplier(double timeoutMultiplier) {
    this.timeoutMultiplier = timeoutMultiplier;
    return this;
  }

  public HttpClientFactory build() {
    HttpClientFactory httpClientFactory = new HttpClientFactory(
      buildClient(),
      Set.copyOf(hostsWithSession),
      contextSupplier,
      callbackExecutor,
      buildUpstreamManager(),
      eventListeners
    );
    ofNullable(metricsConsumer).ifPresent(consumer -> consumer.accept(httpClientFactory.getMetricProvider()));
    return httpClientFactory;
  }

  private AsyncHttpClient buildClient() {
    AsyncHttpClientConfig clientConfig = applyTimeoutMultiplier(configBuilder)
      .setCookieStore(null)
      .build();

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

  public HttpClientFactoryBuilder withUserAgent(String userAgent) {
    configBuilder.setUserAgent(userAgent);
    return this;
  }

  public HttpClientFactoryBuilder withMaxConnections(int maxConnections) {
    configBuilder.setMaxConnections(maxConnections);
    return this;
  }

  public HttpClientFactoryBuilder withMaxRequestRetries(int maxRequestRetries) {
    configBuilder.setMaxRequestRetry(maxRequestRetries);
    return this;
  }

  public HttpClientFactoryBuilder withConnectTimeoutMs(int connectTimeoutMs) {
    configBuilder.setConnectTimeout(connectTimeoutMs);
    return this;
  }

  public HttpClientFactoryBuilder withReadTimeoutMs(int readTimeoutMs) {
    configBuilder.setReadTimeout(readTimeoutMs);
    return this;
  }

  public HttpClientFactoryBuilder withRequestTimeoutMs(int requestTimeoutMs) {
    configBuilder.setRequestTimeout(requestTimeoutMs);
    return this;
  }

  public static final class ConfigKeys {
    private ConfigKeys() {}

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
