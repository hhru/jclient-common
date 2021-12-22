package ru.hh.jclient.common;

import io.netty.handler.ssl.SslContext;
import java.util.ArrayList;
import java.util.List;
import static java.util.Optional.ofNullable;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import ru.hh.jclient.common.metrics.MetricsConsumer;
import ru.hh.jclient.common.util.MDCCopy;
import ru.hh.jclient.common.util.storage.Storage;

public class HttpClientFactoryBuilder {
  public static final double DEFAULT_TIMEOUT_MULTIPLIER = 1;

  private DefaultAsyncHttpClientConfig.Builder configBuilder;
  private RequestStrategy<? extends RequestEngineBuilder<?>> requestStrategy = new DefaultRequestStrategy();
  private Executor callbackExecutor;
  private Storage<HttpClientContext> contextSupplier;
  private double timeoutMultiplier = DEFAULT_TIMEOUT_MULTIPLIER;
  private MetricsConsumer metricsConsumer;
  private final List<HttpClientEventListener> eventListeners;

  public HttpClientFactoryBuilder(Storage<HttpClientContext> contextSupplier, List<HttpClientEventListener> eventListeners) {
    this.configBuilder = new DefaultAsyncHttpClientConfig.Builder();
    this.contextSupplier = contextSupplier;
    this.eventListeners = new ArrayList<>(eventListeners);
  }

  private HttpClientFactoryBuilder(HttpClientFactoryBuilder prototype) {
    this(new DefaultAsyncHttpClientConfig.Builder(prototype.configBuilder.build()),
        prototype.requestStrategy, prototype.callbackExecutor,
        prototype.contextSupplier,
        prototype.timeoutMultiplier,
        prototype.metricsConsumer,
        new ArrayList<>(prototype.eventListeners)
    );
  }

  private HttpClientFactoryBuilder(DefaultAsyncHttpClientConfig.Builder configBuilder,
                                   RequestStrategy<? extends RequestEngineBuilder> requestStrategy,
                                   Executor callbackExecutor,
                                   Storage<HttpClientContext> contextSupplier,
                                   double timeoutMultiplier,
                                   MetricsConsumer metricsConsumer,
                                   List<HttpClientEventListener> eventListeners) {
    this.configBuilder = configBuilder;
    this.requestStrategy = requestStrategy;
    this.callbackExecutor = callbackExecutor;
    this.contextSupplier = contextSupplier;
    this.timeoutMultiplier = timeoutMultiplier;
    this.metricsConsumer = metricsConsumer;
    this.eventListeners = eventListeners;
  }

  public HttpClientFactoryBuilder withProperties(Properties properties) {
    var target = getCopy();
    ofNullable(properties.getProperty(ConfigKeys.MAX_CONNECTIONS)).map(Integer::parseInt)
      .ifPresent(target.configBuilder::setMaxConnections);
    ofNullable(properties.getProperty(ConfigKeys.MAX_REQUEST_RETRIES)).map(Integer::parseInt)
      .ifPresent(target.configBuilder::setMaxRequestRetry);
    ofNullable(properties.getProperty(ConfigKeys.CONNECTION_TIMEOUT_MS)).map(Integer::parseInt)
      .ifPresent(target.configBuilder::setConnectTimeout);
    ofNullable(properties.getProperty(ConfigKeys.READ_TIMEOUT_MS)).map(Integer::parseInt)
      .ifPresent(target.configBuilder::setReadTimeout);
    ofNullable(properties.getProperty(ConfigKeys.REQUEST_TIMEOUT_MS)).map(Integer::parseInt)
      .ifPresent(target.configBuilder::setRequestTimeout);
    ofNullable(properties.getProperty(ConfigKeys.TIMEOUT_MULTIPLIER)).map(Double::parseDouble)
      .ifPresent(timeoutMultiplier -> target.timeoutMultiplier = timeoutMultiplier);
    ofNullable(properties.getProperty(ConfigKeys.USER_AGENT))
      .ifPresent(target.configBuilder::setUserAgent);

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
    var target = getCopy();
    if (!(asyncClientConfig instanceof AsyncHttpClientConfig)) {
      throw new IllegalArgumentException("Argument must be of " + AsyncHttpClientConfig.class.getName());
    }
    target.configBuilder = new DefaultAsyncHttpClientConfig.Builder((AsyncHttpClientConfig) asyncClientConfig);
    return target;
  }

  public HttpClientFactoryBuilder addEventListener(HttpClientEventListener eventListener) {
    var target = getCopy();
    target.eventListeners.add(eventListener);
    return target;
  }

  public HttpClientFactoryBuilder withRequestStrategy(RequestStrategy<? extends RequestEngineBuilder> requestStrategy) {
    var target = getCopy();
    target.requestStrategy = requestStrategy;
    return target;
  }

  public HttpClientFactoryBuilder withThreadFactory(ThreadFactory threadFactory) {
    var target = getCopy();
    target.configBuilder.setThreadFactory(threadFactory);
    return target;
  }

  public HttpClientFactoryBuilder withCallbackExecutor(Executor callbackExecutor) {
    var target = getCopy();
    target.callbackExecutor = callbackExecutor;
    return target;
  }

  public HttpClientFactoryBuilder withStorage(Storage<HttpClientContext> contextSupplier) {
    var target = getCopy();
    target.contextSupplier = contextSupplier;
    return target;
  }

  public HttpClientFactoryBuilder withMetricsConsumer(MetricsConsumer metricsConsumer) {
    var target = getCopy();
    target.metricsConsumer = metricsConsumer;
    return target;
  }

  public HttpClientFactoryBuilder withSSLContext(SslContext sslContext) {
    var target = getCopy();
    target.configBuilder.setSslContext(sslContext);
    return target;
  }

  public HttpClientFactoryBuilder acceptAnyCertificate(boolean enabled) {
    var target = getCopy();
    target.configBuilder.setUseInsecureTrustManager(enabled);
    return target;
  }

  public HttpClientFactoryBuilder withTimeoutMultiplier(double timeoutMultiplier) {
    var target = getCopy();
    target.timeoutMultiplier = timeoutMultiplier;
    return target;
  }

  public HttpClientFactory build() {
    HttpClientFactory httpClientFactory = new HttpClientFactory(
      buildClient(),
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

  private RequestStrategy<? extends RequestEngineBuilder<?>> initStrategy() {
    return requestStrategy.createCustomizedCopy(requestEngineBuilder -> requestEngineBuilder.withTimeoutMultiplier(timeoutMultiplier));
  }

  private DefaultAsyncHttpClientConfig.Builder applyTimeoutMultiplier(DefaultAsyncHttpClientConfig.Builder clientConfigBuilder) {
    AsyncHttpClientConfig config = clientConfigBuilder.build();
    DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder(config);
    builder.setConnectTimeout(config.getConnectTimeout() > 0 ? (int)(config.getConnectTimeout() * timeoutMultiplier) : config.getConnectTimeout());
    builder.setReadTimeout(config.getReadTimeout() > 0 ? (int)(config.getReadTimeout() * timeoutMultiplier) : config.getReadTimeout());
    builder.setRequestTimeout(config.getRequestTimeout() > 0 ? (int)(config.getRequestTimeout() * timeoutMultiplier) : config.getRequestTimeout());
    return builder;
  }

  HttpClientFactoryBuilder getCopy() {
    return new HttpClientFactoryBuilder(this);
  }

  public HttpClientFactoryBuilder withUserAgent(String userAgent) {
    var target = getCopy();
    target.configBuilder.setUserAgent(userAgent);
    return target;
  }

  public HttpClientFactoryBuilder withMaxConnections(int maxConnections) {
    var target = getCopy();
    target.configBuilder.setMaxConnections(maxConnections);
    return target;
  }

  public HttpClientFactoryBuilder withMaxRequestRetries(int maxRequestRetries) {
    var target = getCopy();
    target.configBuilder.setMaxRequestRetry(maxRequestRetries);
    return target;
  }

  public HttpClientFactoryBuilder withConnectTimeoutMs(int connectTimeoutMs) {
    var target = getCopy();
    target.configBuilder.setConnectTimeout(connectTimeoutMs);
    return target;
  }

  public HttpClientFactoryBuilder withReadTimeoutMs(int readTimeoutMs) {
    var target = getCopy();
    target.configBuilder.setReadTimeout(readTimeoutMs);
    return target;
  }

  public HttpClientFactoryBuilder withRequestTimeoutMs(int requestTimeoutMs) {
    var target = getCopy();
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
