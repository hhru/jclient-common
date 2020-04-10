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
  private RequestStrategy<? extends RequestEngineBuilder> requestStrategy = new DefaultRequestStrategy();
  private Executor callbackExecutor;
  private Set<String> hostsWithSession;
  private Storage<HttpClientContext> contextSupplier;
  private double timeoutMultiplier = DEFAULT_TIMEOUT_MULTIPLIER;
  private MetricsConsumer metricsConsumer;
  private final boolean skipCopying;
  private final List<HttpClientEventListener> eventListeners;

  public HttpClientFactoryBuilder(Storage<HttpClientContext> contextSupplier, List<HttpClientEventListener> eventListeners) {
    this.configBuilder = new DefaultAsyncHttpClientConfig.Builder();
    this.contextSupplier = contextSupplier;
    this.eventListeners = new ArrayList<>(eventListeners);
    this.skipCopying = false;
  }

  public HttpClientFactoryBuilder(HttpClientFactoryBuilder prototype, boolean mutable) {
    this(new DefaultAsyncHttpClientConfig.Builder(prototype.configBuilder.build()),
        prototype.requestStrategy, prototype.callbackExecutor,
        ofNullable(prototype.hostsWithSession).map(Set::copyOf).orElse(null),
        prototype.contextSupplier,
        prototype.timeoutMultiplier,
        prototype.metricsConsumer,
        new ArrayList<>(prototype.eventListeners),
        mutable
    );
  }

  private HttpClientFactoryBuilder(DefaultAsyncHttpClientConfig.Builder configBuilder,
                                   RequestStrategy<? extends RequestEngineBuilder> requestStrategy,
                                   Executor callbackExecutor,
                                   Set<String> hostsWithSession, Storage<HttpClientContext> contextSupplier,
                                   double timeoutMultiplier,
                                   MetricsConsumer metricsConsumer,
                                   List<HttpClientEventListener> eventListeners, boolean skipCopying) {
    this.configBuilder = configBuilder;
    this.requestStrategy = requestStrategy;
    this.callbackExecutor = callbackExecutor;
    this.hostsWithSession = hostsWithSession;
    this.contextSupplier = contextSupplier;
    this.timeoutMultiplier = timeoutMultiplier;
    this.metricsConsumer = metricsConsumer;
    this.eventListeners = eventListeners;
    this.skipCopying = skipCopying;
  }

  public HttpClientFactoryBuilder withProperties(Properties properties) {
    var target = getCopyOrSelf();
    ofNullable(properties.getProperty(ConfigKeys.MAX_CONNECTIONS)).map(Integer::parseInt)
      .ifPresent(target::withMaxConnections);
    ofNullable(properties.getProperty(ConfigKeys.MAX_REQUEST_RETRIES)).map(Integer::parseInt)
      .ifPresent(target::withMaxRequestRetries);
    ofNullable(properties.getProperty(ConfigKeys.CONNECTION_TIMEOUT_MS)).map(Integer::parseInt)
      .ifPresent(target::withConnectTimeoutMs);
    ofNullable(properties.getProperty(ConfigKeys.READ_TIMEOUT_MS)).map(Integer::parseInt)
      .ifPresent(target::withReadTimeoutMs);
    ofNullable(properties.getProperty(ConfigKeys.REQUEST_TIMEOUT_MS)).map(Integer::parseInt)
      .ifPresent(target::withRequestTimeoutMs);
    ofNullable(properties.getProperty(ConfigKeys.TIMEOUT_MULTIPLIER)).map(Double::parseDouble)
      .ifPresent(target::withTimeoutMultiplier);
    ofNullable(properties.getProperty(ConfigKeys.USER_AGENT))
      .ifPresent(target::withUserAgent);

    ofNullable(properties.getProperty(ConfigKeys.FOLLOW_REDIRECT)).map(Boolean::parseBoolean)
      .ifPresent(target.configBuilder::setFollowRedirect);
    ofNullable(properties.getProperty(ConfigKeys.COMPRESSION_ENFORCED)).map(Boolean::parseBoolean)
      .ifPresent(target.configBuilder::setCompressionEnforced);
    ofNullable(properties.getProperty(ConfigKeys.ACCEPT_ANY_CERTIFICATE)).map(Boolean::parseBoolean)
      .ifPresent(target.configBuilder::setUseInsecureTrustManager);
    ofNullable(properties.getProperty(ConfigKeys.KEEP_ALIVE)).map(Boolean::parseBoolean)
      .ifPresent(target.configBuilder::setKeepAlive);
    ofNullable(properties.getProperty(ConfigKeys.IO_THREADS_COUNT)).map(Integer::parseInt)
      .ifPresent(target.configBuilder::setIoThreadsCount);

    return target;
  }

  /**
   * use this only if there's not enough "with*" methods to cover all requirements
   * @param asyncClientConfig instance of {@link DefaultAsyncHttpClientConfig}
   * @return instance of HttpClientFactoryBuilder based on passed config to continue building
   */
  public HttpClientFactoryBuilder withNativeConfig(Object asyncClientConfig) {
    var target = getCopyOrSelf();
    if (!(asyncClientConfig instanceof AsyncHttpClientConfig)) {
      throw new IllegalArgumentException("Argument must be of " + AsyncHttpClientConfig.class.getName());
    }
    target.configBuilder = new DefaultAsyncHttpClientConfig.Builder((AsyncHttpClientConfig) asyncClientConfig);
    return target;
  }

  public HttpClientFactoryBuilder addEventListener(HttpClientEventListener eventListener) {
    var target = getCopyOrSelf();
    target.eventListeners.add(eventListener);
    return target;
  }

  public HttpClientFactoryBuilder withRequestStrategy(RequestStrategy<? extends RequestEngineBuilder> requestStrategy) {
    var target = getCopyOrSelf();
    target.requestStrategy = requestStrategy;
    return target;
  }

  public HttpClientFactoryBuilder withThreadFactory(ThreadFactory threadFactory) {
    var target = getCopyOrSelf();
    target.configBuilder.setThreadFactory(threadFactory);
    return target;
  }

  public HttpClientFactoryBuilder withCallbackExecutor(Executor callbackExecutor) {
    var target = getCopyOrSelf();
    target.callbackExecutor = callbackExecutor;
    return target;
  }

  public HttpClientFactoryBuilder withHostsWithSession(Collection<String> hostsWithSession) {
    var target = getCopyOrSelf();
    target.hostsWithSession = new HashSet<>(hostsWithSession);
    return target;
  }

  public HttpClientFactoryBuilder withStorage(Storage<HttpClientContext> contextSupplier) {
    var target = getCopyOrSelf();
    target.contextSupplier = contextSupplier;
    return target;
  }

  public HttpClientFactoryBuilder withMetricsConsumer(MetricsConsumer metricsConsumer) {
    var target = getCopyOrSelf();
    target.metricsConsumer = metricsConsumer;
    return target;
  }

  public HttpClientFactoryBuilder withSSLContext(SslContext sslContext) {
    var target = getCopyOrSelf();
    target.configBuilder.setSslContext(sslContext);
    return target;
  }

  public HttpClientFactoryBuilder acceptAnyCertificate(boolean enabled) {
    var target = getCopyOrSelf();
    target.configBuilder.setUseInsecureTrustManager(enabled);
    return target;
  }

  public HttpClientFactoryBuilder withTimeoutMultiplier(double timeoutMultiplier) {
    var target = getCopyOrSelf();
    target.timeoutMultiplier = timeoutMultiplier;
    return target;
  }

  public HttpClientFactory build() {
    HttpClientFactory httpClientFactory = new HttpClientFactory(
      buildClient(),
      ofNullable(hostsWithSession).map(Set::copyOf).orElseGet(Set::of),
      contextSupplier,
      callbackExecutor,
      initStrategy(),
      List.copyOf(eventListeners)
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

  private RequestStrategy<? extends RequestEngineBuilder> initStrategy() {
    requestStrategy.setTimeoutMultiplier(timeoutMultiplier);
    return requestStrategy;
  }

  private DefaultAsyncHttpClientConfig.Builder applyTimeoutMultiplier(DefaultAsyncHttpClientConfig.Builder clientConfigBuilder) {
    AsyncHttpClientConfig config = clientConfigBuilder.build();
    DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder(config);
    builder.setConnectTimeout(config.getConnectTimeout() > 0 ? (int)(config.getConnectTimeout() * timeoutMultiplier) : config.getConnectTimeout());
    builder.setReadTimeout(config.getReadTimeout() > 0 ? (int)(config.getReadTimeout() * timeoutMultiplier) : config.getReadTimeout());
    builder.setRequestTimeout(config.getRequestTimeout() > 0 ? (int)(config.getRequestTimeout() * timeoutMultiplier) : config.getRequestTimeout());
    return builder;
  }
  
  private HttpClientFactoryBuilder getCopyOrSelf() {
    return skipCopying ? this : new HttpClientFactoryBuilder(this, skipCopying);
  }

  public HttpClientFactoryBuilder withUserAgent(String userAgent) {
    var target = getCopyOrSelf();
    target.configBuilder.setUserAgent(userAgent);
    return target;
  }

  public HttpClientFactoryBuilder withMaxConnections(int maxConnections) {
    var target = getCopyOrSelf();
    target.configBuilder.setMaxConnections(maxConnections);
    return target;
  }

  public HttpClientFactoryBuilder withMaxRequestRetries(int maxRequestRetries) {
    var target = getCopyOrSelf();
    target.configBuilder.setMaxRequestRetry(maxRequestRetries);
    return target;
  }

  public HttpClientFactoryBuilder withConnectTimeoutMs(int connectTimeoutMs) {
    var target = getCopyOrSelf();
    target.configBuilder.setConnectTimeout(connectTimeoutMs);
    return target;
  }

  public HttpClientFactoryBuilder withReadTimeoutMs(int readTimeoutMs) {
    var target = getCopyOrSelf();
    target.configBuilder.setReadTimeout(readTimeoutMs);
    return target;
  }

  public HttpClientFactoryBuilder withRequestTimeoutMs(int requestTimeoutMs) {
    var target = getCopyOrSelf();
    target.configBuilder.setRequestTimeout(requestTimeoutMs);
    return target;
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
