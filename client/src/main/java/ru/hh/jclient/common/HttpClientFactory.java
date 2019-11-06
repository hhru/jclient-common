package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;

import org.asynchttpclient.AsyncHttpClient;
import ru.hh.jclient.common.metrics.MetricsProvider;
import ru.hh.jclient.common.util.storage.Storage;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executor;

public class HttpClientFactory {

  private final AsyncHttpClient http;
  private final Set<String> hostsWithSession;
  private final Storage<HttpClientContext> contextSupplier;
  private final Executor callbackExecutor;
  private final UpstreamManager upstreamManager;
  private final List<HttpClientEventListener> eventListeners;

  public HttpClientFactory(AsyncHttpClient http, Set<String> hostsWithSession, Storage<HttpClientContext> contextSupplier) {
    this(http, hostsWithSession, contextSupplier, Runnable::run);
  }

  public HttpClientFactory(AsyncHttpClient http,
                           Set<String> hostsWithSession,
                           Storage<HttpClientContext> contextSupplier,
                           Executor callbackExecutor) {
    this(http, hostsWithSession, contextSupplier, callbackExecutor, new DefaultUpstreamManager());
  }

  public HttpClientFactory(AsyncHttpClient http,
                           Set<String> hostsWithSession,
                           Storage<HttpClientContext> contextSupplier,
                           Executor callbackExecutor,
                           UpstreamManager upstreamManager) {
    this(http, hostsWithSession, contextSupplier, callbackExecutor, upstreamManager, List.of());
  }

  public HttpClientFactory(AsyncHttpClient http,
                           Set<String> hostsWithSession,
                           Storage<HttpClientContext> contextSupplier,
                           Executor callbackExecutor,
                           UpstreamManager upstreamManager,
                           List<HttpClientEventListener> eventListeners) {
    this.http = requireNonNull(http, "http must not be null");
    this.hostsWithSession = requireNonNull(hostsWithSession, "hostsWithSession must not be null");
    this.contextSupplier = requireNonNull(contextSupplier, "contextSupplier must not be null");
    this.callbackExecutor = requireNonNull(callbackExecutor, "callbackExecutor must not be null");
    this.upstreamManager = requireNonNull(upstreamManager, "upstreamManager must not be null");
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
        hostsWithSession,
        upstreamManager,
        contextSupplier,
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
}
