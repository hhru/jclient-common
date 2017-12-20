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

//TODO rename to HttpClientFactoryBuilder
public class HttpClientBuilderWizard {

  private final AsyncHttpClientConfig.Builder configBuilder;
  private final UpstreamManager upstreamManager;

  private Executor callbackExecutor;
  private Set<String> hostsWithSession;
  private Storage<HttpClientContext> contextSupplier;


  public static HttpClientBuilderWizard of(UpstreamManager upstreamManager) {
    return new HttpClientBuilderWizard(upstreamManager, new AsyncHttpClientConfig.Builder());
  } 

  private HttpClientBuilderWizard(UpstreamManager upstreamManager, AsyncHttpClientConfig.Builder configBuilder) {
    this.configBuilder = configBuilder;
    this.upstreamManager = upstreamManager;
  }

  public HttpClientBuilder build() {
    return new HttpClientBuilder(
        buildClient(),
        hostsWithSession,
        contextSupplier,
        callbackExecutor,
        upstreamManager
    );
  }

  private AsyncHttpClient buildClient() {
    AsyncHttpClientConfig clientConfig = configBuilder.build();
    return new AsyncHttpClient(new NettyAsyncHttpProvider(clientConfig), clientConfig);
  }

  public HttpClientBuilderWizard withCallbackExecutor(Executor callbackExecutor) {
    this.callbackExecutor = callbackExecutor;
    return this;
  }

  public HttpClientBuilderWizard withHostsWithSession(Collection<String> hostsWithSession) {
    this.hostsWithSession = new HashSet<>(hostsWithSession);
    return this;
  }

  public HttpClientBuilderWizard withStorage(Storage<HttpClientContext> contextSupplier) {
    this.contextSupplier = contextSupplier;
    return this;
  }

  //below are AsyncHttpClientConfig.Builder method delegates

  public HttpClientBuilderWizard setMaxConnections(int maxConnections) {
    this.configBuilder.setMaxConnections(maxConnections);
    return this;
  }

  public HttpClientBuilderWizard setMaxConnectionsPerHost(int maxConnectionsPerHost) {
    this.configBuilder.setMaxConnectionsPerHost(maxConnectionsPerHost);
    return this;
  }

  public HttpClientBuilderWizard setConnectTimeout(int connectTimeOut) {
    this.configBuilder.setConnectTimeout(connectTimeOut);
    return this;
  }

  public HttpClientBuilderWizard setWebSocketTimeout(int webSocketTimeout) {
    this.configBuilder.setWebSocketTimeout(webSocketTimeout);
    return this;
  }

  public HttpClientBuilderWizard setReadTimeout(int readTimeout) {
    this.configBuilder.setReadTimeout(readTimeout);
    return this;
  }

  public HttpClientBuilderWizard setPooledConnectionIdleTimeout(int pooledConnectionIdleTimeout) {
    this.configBuilder.setPooledConnectionIdleTimeout(pooledConnectionIdleTimeout);
    return this;
  }

  public HttpClientBuilderWizard setRequestTimeout(int requestTimeout) {
    this.configBuilder.setRequestTimeout(requestTimeout);
    return this;
  }

  public HttpClientBuilderWizard setFollowRedirect(boolean followRedirect) {
    this.configBuilder.setFollowRedirect(followRedirect);
    return this;
  }

  public HttpClientBuilderWizard setMaxRedirects(int maxRedirects) {
    this.configBuilder.setMaxRedirects(maxRedirects);
    return this;
  }

  public HttpClientBuilderWizard setCompressionEnforced(boolean compressionEnforced) {
    this.configBuilder.setCompressionEnforced(compressionEnforced);
    return this;
  }

  public HttpClientBuilderWizard setUserAgent(String userAgent) {
    this.configBuilder.setUserAgent(userAgent);
    return this;
  }

  public HttpClientBuilderWizard setAllowPoolingConnections(boolean allowPoolingConnections) {
    this.configBuilder.setAllowPoolingConnections(allowPoolingConnections);
    return this;
  }

  public HttpClientBuilderWizard setExecutorService(ExecutorService applicationThreadPool) {
    this.configBuilder.setExecutorService(applicationThreadPool);
    return this;
  }

  public HttpClientBuilderWizard setProxyServerSelector(ProxyServerSelector proxyServerSelector) {
    this.configBuilder.setProxyServerSelector(proxyServerSelector);
    return this;
  }

  public HttpClientBuilderWizard setProxyServer(ProxyServer proxyServer) {
    this.configBuilder.setProxyServer(proxyServer);
    return this;
  }

  public HttpClientBuilderWizard setSSLContext(SSLContext sslContext) {
    this.configBuilder.setSSLContext(sslContext);
    return this;
  }

  public HttpClientBuilderWizard setAsyncHttpClientProviderConfig(AsyncHttpProviderConfig<?, ?> providerConfig) {
    this.configBuilder.setAsyncHttpClientProviderConfig(providerConfig);
    return this;
  }

  public HttpClientBuilderWizard setRealm(Realm realm) {
    this.configBuilder.setRealm(realm);
    return this;
  }

  public HttpClientBuilderWizard addRequestFilter(RequestFilter requestFilter) {
    this.configBuilder.addRequestFilter(requestFilter);
    return this;
  }

  public HttpClientBuilderWizard removeRequestFilter(RequestFilter requestFilter) {
    this.configBuilder.removeRequestFilter(requestFilter);
    return this;
  }

  public HttpClientBuilderWizard addResponseFilter(ResponseFilter responseFilter) {
    this.configBuilder.addResponseFilter(responseFilter);
    return this;
  }

  public HttpClientBuilderWizard removeResponseFilter(ResponseFilter responseFilter) {
    this.configBuilder.removeResponseFilter(responseFilter);
    return this;
  }

  public HttpClientBuilderWizard addIOExceptionFilter(IOExceptionFilter ioExceptionFilter) {
    this.configBuilder.addIOExceptionFilter(ioExceptionFilter);
    return this;
  }

  public HttpClientBuilderWizard removeIOExceptionFilter(IOExceptionFilter ioExceptionFilter) {
    this.configBuilder.removeIOExceptionFilter(ioExceptionFilter);
    return this;
  }

  public HttpClientBuilderWizard setMaxRequestRetry(int maxRequestRetry) {
    this.configBuilder.setMaxRequestRetry(maxRequestRetry);
    return this;
  }

  public HttpClientBuilderWizard setAllowPoolingSslConnections(boolean allowPoolingSslConnections) {
    this.configBuilder.setAllowPoolingSslConnections(allowPoolingSslConnections);
    return this;
  }

  public HttpClientBuilderWizard setDisableUrlEncodingForBoundedRequests(boolean disableUrlEncodingForBoundedRequests) {
    this.configBuilder.setDisableUrlEncodingForBoundedRequests(disableUrlEncodingForBoundedRequests);
    return this;
  }

  public HttpClientBuilderWizard setUseProxySelector(boolean useProxySelector) {
    this.configBuilder.setUseProxySelector(useProxySelector);
    return this;
  }

  public HttpClientBuilderWizard setUseProxyProperties(boolean useProxyProperties) {
    this.configBuilder.setUseProxyProperties(useProxyProperties);
    return this;
  }

  public HttpClientBuilderWizard setIOThreadMultiplier(int multiplier) {
    this.configBuilder.setIOThreadMultiplier(multiplier);
    return this;
  }

  public HttpClientBuilderWizard setHostnameVerifier(HostnameVerifier hostnameVerifier) {
    this.configBuilder.setHostnameVerifier(hostnameVerifier);
    return this;
  }

  public HttpClientBuilderWizard setStrict302Handling(boolean strict302Handling) {
    this.configBuilder.setStrict302Handling(strict302Handling);
    return this;
  }

  public HttpClientBuilderWizard setUseRelativeURIsWithConnectProxies(boolean useRelativeURIsWithConnectProxies) {
    this.configBuilder.setUseRelativeURIsWithConnectProxies(useRelativeURIsWithConnectProxies);
    return this;
  }

  public HttpClientBuilderWizard setConnectionTTL(int connectionTTL) {
    this.configBuilder.setConnectionTTL(connectionTTL);
    return this;
  }

  public HttpClientBuilderWizard setAcceptAnyCertificate(boolean acceptAnyCertificate) {
    this.configBuilder.setAcceptAnyCertificate(acceptAnyCertificate);
    return this;
  }

  public HttpClientBuilderWizard setEnabledProtocols(String[] enabledProtocols) {
    this.configBuilder.setEnabledProtocols(enabledProtocols);
    return this;
  }

  public HttpClientBuilderWizard setEnabledCipherSuites(String[] enabledCipherSuites) {
    this.configBuilder.setEnabledCipherSuites(enabledCipherSuites);
    return this;
  }

  public HttpClientBuilderWizard setSslSessionCacheSize(Integer sslSessionCacheSize) {
    this.configBuilder.setSslSessionCacheSize(sslSessionCacheSize);
    return this;
  }

  public HttpClientBuilderWizard setSslSessionTimeout(Integer sslSessionTimeout) {
    this.configBuilder.setSslSessionTimeout(sslSessionTimeout);
    return this;
  }
}
