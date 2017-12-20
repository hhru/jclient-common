package ru.hh.jclient.common;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpProviderConfig;
import com.ning.http.client.filter.RequestFilter;
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider;
import ru.hh.jclient.common.util.storage.Storage;

import javax.net.ssl.SSLContext;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static java.util.Optional.ofNullable;

//TODO rename to HttpClientFactoryBuilder
public final class HttpClientConfig {

  private final AsyncHttpClientConfig.Builder configBuilder;

  private UpstreamManager upstreamManager;
  private Executor callbackExecutor;
  private Set<String> hostsWithSession;
  private Storage<HttpClientContext> contextSupplier;
  private Consumer<MetricProvider> monitoringConnector;

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
    return new HttpClientConfig(configBuilder);
  }

  /**
   * use this only if there's not enough "with*" methods to cover all requirements
   * example: you need to set {@link AsyncHttpProviderConfig}
   * example: you need to set {@link RequestFilter}
   * @param asyncClientConfigBuilder instance of {@link AsyncHttpClientConfig.Builder}
   * @return instance of HttpClientConfig based on passed config to continue building
   */
  public static HttpClientConfig forNativeBuilder(Object asyncClientConfigBuilder) {
    if (!(asyncClientConfigBuilder instanceof AsyncHttpClientConfig.Builder)) {
      throw new IllegalArgumentException("Argument must be of " + AsyncHttpClientConfig.Builder.class.getName());
    }
    return new HttpClientConfig((AsyncHttpClientConfig.Builder) asyncClientConfigBuilder);
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

  public HttpClientConfig withHostsWithSession(Collection<String> hostsWithSession) {
    this.hostsWithSession = new HashSet<>(hostsWithSession);
    return this;
  }

  public HttpClientConfig withStorage(Storage<HttpClientContext> contextSupplier) {
    this.contextSupplier = contextSupplier;
    return this;
  }

  public HttpClientConfig withMonitoringConnector(Consumer<MetricProvider> monitoringConnector) {
    this.monitoringConnector = monitoringConnector;
    return this;
  }

  public HttpClientConfig withSSLContext(SSLContext sslContext) {
    this.configBuilder.setSSLContext(sslContext);
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
    connectMonitoringIfPossible(http);
    return httpClientBuilder;
  }

  private void connectMonitoringIfPossible(AsyncHttpClient http) {
    if (monitoringConnector == null) {
      return;
    }
    monitoringConnector.accept(MetricProviderFactory.from(http, this));
  }

  private AsyncHttpClient buildClient() {
    AsyncHttpClientConfig clientConfig = configBuilder.build();
    return new AsyncHttpClient(new NettyAsyncHttpProvider(clientConfig), clientConfig);
  }

  public interface ConfigKeys {
    String USER_AGENT = "userAgent";

    String MAX_CONNECTIONS = "maxTotalConnections";
    String MAX_REQUEST_RETRIES = "maxRequestRetries";

    String CONNECTION_TIMEOUT_MS = "connectionTimeoutMs";
    String READ_TIMEOUT_MS = "readTimeoutMs";
    String REQUEST_TIMEOUT_MS = "requestTimeoutMs";

    String FOLLOW_REDIRECT = "followRedirect";
    String COMPRESSION_ENFORCED = "compressionEnforced";
    String ALLOW_POOLING_CONNECTIONS = "allowPoolingConnections";
  }
}
