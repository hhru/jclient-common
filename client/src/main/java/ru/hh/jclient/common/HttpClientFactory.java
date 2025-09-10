package ru.hh.jclient.common;

import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.function.UnaryOperator;
import org.asynchttpclient.AsyncHttpClient;
import ru.hh.jclient.common.metrics.MetricsProvider;
import ru.hh.jclient.common.util.storage.Storage;
import ru.hh.trace.TraceContext;

public class HttpClientFactory {

  private final AsyncHttpClient http;
  private final Storage<HttpClientContext> contextSupplier;
  private final Set<String> customHostsWithSession;
  private final Executor callbackExecutor;
  private final RequestStrategy<? extends RequestEngineBuilder<?>> requestStrategy;
  private final TraceContext traceContext;

  public HttpClientFactory(AsyncHttpClient http, Storage<HttpClientContext> contextSupplier, TraceContext traceContext) {
    this(http, contextSupplier, Set.of(), traceContext);
  }

  public HttpClientFactory(
      AsyncHttpClient http,
      Storage<HttpClientContext> contextSupplier,
      Set<String> customHostsWithSession,
      TraceContext traceContext
  ) {
    this(http, contextSupplier, customHostsWithSession, Runnable::run, traceContext);
  }

  public HttpClientFactory(
      AsyncHttpClient http,
      Storage<HttpClientContext> contextSupplier,
      Set<String> customHostsWithSession,
      Executor callbackExecutor,
      TraceContext traceContext
  ) {
    this(http, contextSupplier, customHostsWithSession, callbackExecutor, new DefaultRequestStrategy(), traceContext);
  }

  public HttpClientFactory(
      AsyncHttpClient http,
      Storage<HttpClientContext> contextSupplier,
      Set<String> customHostsWithSession,
      Executor callbackExecutor,
      RequestStrategy<?> requestStrategy,
      TraceContext traceContext
  ) {
    this.http = requireNonNull(http, "http must not be null");
    this.contextSupplier = requireNonNull(contextSupplier, "contextSupplier must not be null");
    this.customHostsWithSession = requireNonNull(customHostsWithSession, "hostsWithSession must not be null");
    this.callbackExecutor = requireNonNull(callbackExecutor, "callbackExecutor must not be null");
    this.requestStrategy = requireNonNull(requestStrategy, "upstreamManager must not be null");
    this.traceContext = requireNonNull(traceContext, "traceContext must not be null");
  }

  /**
   * Specifies request to be executed. This is a starting point of request execution chain.
   *
   * @param request to execute
   */
  public HttpClient with(Request request) {
    return new HttpClientImpl(
        http,
        requireNonNull(request, "request must not be null"),
        requestStrategy,
        contextSupplier,
        customHostsWithSession,
        callbackExecutor,
        traceContext
    );
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
   *
   * @param mapper action to customize {@link RequestStrategy}
   * @return new instance of httpClientFactory
   * @throws ClassCastException if strategy type differs from required customization
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public HttpClientFactory createCustomizedCopy(UnaryOperator<? extends RequestEngineBuilder> mapper) {
    return new HttpClientFactory(
        this.http,
        this.contextSupplier,
        this.customHostsWithSession,
        this.callbackExecutor,
        this.requestStrategy.createCustomizedCopy((UnaryOperator) mapper),
        this.traceContext
    );
  }
}
