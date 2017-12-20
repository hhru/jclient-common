package ru.hh.jclient.common;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpProviderConfig;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.ProxyServerSelector;
import com.ning.http.client.Realm;
import com.ning.http.client.filter.IOExceptionFilter;
import com.ning.http.client.filter.RequestFilter;
import com.ning.http.client.filter.ResponseFilter;
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider;
import ru.hh.jclient.common.util.storage.Storage;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.function.Supplier;

//TODO rename to HttpClientFactoryBuilder
public class HttpClientConfig {

  private final AsyncHttpClientConfig.Builder configBuilder;
  private final UpstreamManager upstreamManager;

  private Executor callbackExecutor;
  private Set<String> hostsWithSession;
  private Storage<HttpClientContext> contextSupplier;
  private Consumer<MetricProvider> monitoringConnector;


  public static HttpClientConfig of(UpstreamManager upstreamManager) {
    return new HttpClientConfig(upstreamManager, new AsyncHttpClientConfig.Builder());
  }

  private HttpClientConfig(UpstreamManager upstreamManager, AsyncHttpClientConfig.Builder configBuilder) {
    this.configBuilder = configBuilder;
    this.upstreamManager = upstreamManager;
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

  public HttpClientBuilder build() {
    AsyncHttpClient http = buildClient();
    HttpClientBuilder httpClientBuilder = new HttpClientBuilder(
        http,
        hostsWithSession,
        contextSupplier,
        callbackExecutor,
        upstreamManager
    );
    connectHttpClientMonitoringIfPossible(http);
    return httpClientBuilder;
  }

  private void connectHttpClientMonitoringIfPossible(AsyncHttpClient http) {
    if (monitoringConnector == null) {
      return;
    }
    ExecutorService executorService = http.getConfig().executorService();
    if (!(executorService instanceof ThreadPoolExecutor)) {
      monitoringConnector.accept(MetricProvider.EMPTY);
    }
    monitoringConnector.accept(new MetricProvider() {
      @Override
      public Supplier<Integer> threadPoolSizeProvider() {
        return ((ThreadPoolExecutor)executorService)::getPoolSize;
      }

      @Override
      public Supplier<Integer> threadPoolActiveTaskSizeProvider() {
        return ((ThreadPoolExecutor)executorService)::getActiveCount;
      }

      @Override
      public boolean containsThreadMetrics() {
        return true;
      }
    });
  }

  private AsyncHttpClient buildClient() {
    AsyncHttpClientConfig clientConfig = configBuilder.build();
    return new AsyncHttpClient(new NettyAsyncHttpProvider(clientConfig), clientConfig);
  }

  //below are AsyncHttpClientConfig.Builder method delegates

  public HttpClientConfig setMaxConnections(int maxConnections) {
    this.configBuilder.setMaxConnections(maxConnections);
    return this;
  }

  public HttpClientConfig setMaxConnectionsPerHost(int maxConnectionsPerHost) {
    this.configBuilder.setMaxConnectionsPerHost(maxConnectionsPerHost);
    return this;
  }

  public HttpClientConfig setConnectTimeout(int connectTimeOut) {
    this.configBuilder.setConnectTimeout(connectTimeOut);
    return this;
  }

  public HttpClientConfig setWebSocketTimeout(int webSocketTimeout) {
    this.configBuilder.setWebSocketTimeout(webSocketTimeout);
    return this;
  }

  public HttpClientConfig setReadTimeout(int readTimeout) {
    this.configBuilder.setReadTimeout(readTimeout);
    return this;
  }

  public HttpClientConfig setPooledConnectionIdleTimeout(int pooledConnectionIdleTimeout) {
    this.configBuilder.setPooledConnectionIdleTimeout(pooledConnectionIdleTimeout);
    return this;
  }

  public HttpClientConfig setRequestTimeout(int requestTimeout) {
    this.configBuilder.setRequestTimeout(requestTimeout);
    return this;
  }

  public HttpClientConfig setFollowRedirect(boolean followRedirect) {
    this.configBuilder.setFollowRedirect(followRedirect);
    return this;
  }

  public HttpClientConfig setMaxRedirects(int maxRedirects) {
    this.configBuilder.setMaxRedirects(maxRedirects);
    return this;
  }

  public HttpClientConfig setCompressionEnforced(boolean compressionEnforced) {
    this.configBuilder.setCompressionEnforced(compressionEnforced);
    return this;
  }

  public HttpClientConfig setUserAgent(String userAgent) {
    this.configBuilder.setUserAgent(userAgent);
    return this;
  }

  public HttpClientConfig setAllowPoolingConnections(boolean allowPoolingConnections) {
    this.configBuilder.setAllowPoolingConnections(allowPoolingConnections);
    return this;
  }

  public HttpClientConfig setExecutorService(ExecutorService applicationThreadPool) {
    this.configBuilder.setExecutorService(applicationThreadPool);
    return this;
  }

  public HttpClientConfig setProxyServerSelector(ProxyServerSelector proxyServerSelector) {
    this.configBuilder.setProxyServerSelector(proxyServerSelector);
    return this;
  }

  public HttpClientConfig setProxyServer(ProxyServer proxyServer) {
    this.configBuilder.setProxyServer(proxyServer);
    return this;
  }

  public HttpClientConfig setSSLContext(SSLContext sslContext) {
    this.configBuilder.setSSLContext(sslContext);
    return this;
  }

  public HttpClientConfig setAsyncHttpClientProviderConfig(AsyncHttpProviderConfig<?, ?> providerConfig) {
    this.configBuilder.setAsyncHttpClientProviderConfig(providerConfig);
    return this;
  }

  public HttpClientConfig setRealm(Realm realm) {
    this.configBuilder.setRealm(realm);
    return this;
  }

  public HttpClientConfig addRequestFilter(RequestFilter requestFilter) {
    this.configBuilder.addRequestFilter(requestFilter);
    return this;
  }

  public HttpClientConfig removeRequestFilter(RequestFilter requestFilter) {
    this.configBuilder.removeRequestFilter(requestFilter);
    return this;
  }

  public HttpClientConfig addResponseFilter(ResponseFilter responseFilter) {
    this.configBuilder.addResponseFilter(responseFilter);
    return this;
  }

  public HttpClientConfig removeResponseFilter(ResponseFilter responseFilter) {
    this.configBuilder.removeResponseFilter(responseFilter);
    return this;
  }

  public HttpClientConfig addIOExceptionFilter(IOExceptionFilter ioExceptionFilter) {
    this.configBuilder.addIOExceptionFilter(ioExceptionFilter);
    return this;
  }

  public HttpClientConfig removeIOExceptionFilter(IOExceptionFilter ioExceptionFilter) {
    this.configBuilder.removeIOExceptionFilter(ioExceptionFilter);
    return this;
  }

  public HttpClientConfig setMaxRequestRetry(int maxRequestRetry) {
    this.configBuilder.setMaxRequestRetry(maxRequestRetry);
    return this;
  }

  public HttpClientConfig setAllowPoolingSslConnections(boolean allowPoolingSslConnections) {
    this.configBuilder.setAllowPoolingSslConnections(allowPoolingSslConnections);
    return this;
  }

  public HttpClientConfig setDisableUrlEncodingForBoundedRequests(boolean disableUrlEncodingForBoundedRequests) {
    this.configBuilder.setDisableUrlEncodingForBoundedRequests(disableUrlEncodingForBoundedRequests);
    return this;
  }

  public HttpClientConfig setUseProxySelector(boolean useProxySelector) {
    this.configBuilder.setUseProxySelector(useProxySelector);
    return this;
  }

  public HttpClientConfig setUseProxyProperties(boolean useProxyProperties) {
    this.configBuilder.setUseProxyProperties(useProxyProperties);
    return this;
  }

  public HttpClientConfig setIOThreadMultiplier(int multiplier) {
    this.configBuilder.setIOThreadMultiplier(multiplier);
    return this;
  }

  public HttpClientConfig setHostnameVerifier(HostnameVerifier hostnameVerifier) {
    this.configBuilder.setHostnameVerifier(hostnameVerifier);
    return this;
  }

  public HttpClientConfig setStrict302Handling(boolean strict302Handling) {
    this.configBuilder.setStrict302Handling(strict302Handling);
    return this;
  }

  public HttpClientConfig setUseRelativeURIsWithConnectProxies(boolean useRelativeURIsWithConnectProxies) {
    this.configBuilder.setUseRelativeURIsWithConnectProxies(useRelativeURIsWithConnectProxies);
    return this;
  }

  public HttpClientConfig setConnectionTTL(int connectionTTL) {
    this.configBuilder.setConnectionTTL(connectionTTL);
    return this;
  }

  public HttpClientConfig setAcceptAnyCertificate(boolean acceptAnyCertificate) {
    this.configBuilder.setAcceptAnyCertificate(acceptAnyCertificate);
    return this;
  }

  public HttpClientConfig setEnabledProtocols(String[] enabledProtocols) {
    this.configBuilder.setEnabledProtocols(enabledProtocols);
    return this;
  }

  public HttpClientConfig setEnabledCipherSuites(String[] enabledCipherSuites) {
    this.configBuilder.setEnabledCipherSuites(enabledCipherSuites);
    return this;
  }

  public HttpClientConfig setSslSessionCacheSize(Integer sslSessionCacheSize) {
    this.configBuilder.setSslSessionCacheSize(sslSessionCacheSize);
    return this;
  }

  public HttpClientConfig setSslSessionTimeout(Integer sslSessionTimeout) {
    this.configBuilder.setSslSessionTimeout(sslSessionTimeout);
    return this;
  }
}
