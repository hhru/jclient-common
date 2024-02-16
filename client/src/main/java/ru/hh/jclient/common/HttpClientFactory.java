package ru.hh.jclient.common;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import org.asynchttpclient.AsyncHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.metrics.MetricsProvider;
import ru.hh.jclient.common.util.storage.Storage;

public class HttpClientFactory implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientFactory.class);


  private final AsyncHttpClient http;
  private final Storage<HttpClientContext> contextSupplier;
  private final Set<String> customHostsWithSession;
  private final Executor callbackExecutor;
  private final RequestStrategy<? extends RequestEngineBuilder<?>> requestStrategy;
  private final List<HttpClientEventListener> eventListeners;

  public HttpClientFactory(AsyncHttpClient http, Storage<HttpClientContext> contextSupplier) {
    this(http, contextSupplier, Set.of());
  }

  public HttpClientFactory(AsyncHttpClient http, Storage<HttpClientContext> contextSupplier, Set<String> customHostsWithSession) {
    this(http, contextSupplier, customHostsWithSession, Runnable::run);
  }

  public HttpClientFactory(AsyncHttpClient http,
                           Storage<HttpClientContext> contextSupplier,
                           Set<String> customHostsWithSession,
                           Executor callbackExecutor) {
    this(http, contextSupplier, customHostsWithSession, callbackExecutor, new DefaultRequestStrategy());
  }

  public HttpClientFactory(AsyncHttpClient http,
                           Storage<HttpClientContext> contextSupplier,
                           Set<String> customHostsWithSession,
                           Executor callbackExecutor,
                           RequestStrategy<?> requestStrategy) {
    this(http, contextSupplier, customHostsWithSession, callbackExecutor, requestStrategy, List.of());
  }

  public HttpClientFactory(AsyncHttpClient http,
                           Storage<HttpClientContext> contextSupplier,
                           Set<String> customHostsWithSession,
                           Executor callbackExecutor,
                           RequestStrategy<?> requestStrategy,
                           List<HttpClientEventListener> eventListeners) {
    this.http = requireNonNull(http, "http must not be null");
    this.contextSupplier = requireNonNull(contextSupplier, "contextSupplier must not be null");
    this.customHostsWithSession = requireNonNull(customHostsWithSession, "hostsWithSession must not be null");
    this.callbackExecutor = requireNonNull(callbackExecutor, "callbackExecutor must not be null");
    this.requestStrategy = requireNonNull(requestStrategy, "upstreamManager must not be null");
    this.eventListeners = eventListeners;
  }

  /**
   * Specifies request to be executed. This is a starting point of request execution chain.
   *
   * @param request
   *          to execute
   */
  public HttpClient with(Request request) {
    return new HttpClientImpl(
        http,
        requireNonNull(request, "request must not be null"),
        requestStrategy,
        contextSupplier,
        customHostsWithSession,
        callbackExecutor,
      eventListeners);
  }

  /**
   * @return returns copy (within case insensitive map) of headers contained within global (incoming) request
   */
  public Map<String, List<String>> getHeaders() {
    Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    headers.putAll(contextSupplier.get().getHeaders());
    return headers;
  }

  AsyncHttpClient getHttp() {
    return http;
  }

  MetricsProvider getMetricProvider() {
    return MetricsProviderFactory.from(getHttp());
  }

  Storage<HttpClientContext> getContextSupplier() {
    return contextSupplier;
  }

  /**
   * create customized copy of the factory
   * @param mapper action to customize {@link RequestStrategy}
   * @return new instance of httpClientFactory
   * @throws ClassCastException if strategy type differs from required customization
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public HttpClientFactory createCustomizedCopy(UnaryOperator<? extends RequestEngineBuilder> mapper) {
    return new HttpClientFactory(this.http, this.contextSupplier, this.customHostsWithSession, this.callbackExecutor,
                                 this.requestStrategy.createCustomizedCopy((UnaryOperator) mapper),
                                 this.eventListeners);
  }

  /**
   * Close all internal resources
   */
  @Override
  public void close() {
    try {
      this.http.close();
    } catch (IOException e) {
      LOGGER.error("Error occurred during closing HttpClientFactory", e);
    }

    if (this.callbackExecutor instanceof ExecutorService executorService) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
          executorService.shutdownNow();

          if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
            LOGGER.error("callbackExecutor didn't terminate");
          }
        }
      } catch (InterruptedException ie) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }
}
