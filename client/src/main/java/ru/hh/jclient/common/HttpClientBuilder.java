package ru.hh.jclient.common;

import com.google.common.collect.ImmutableSet;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import static java.util.Objects.requireNonNull;

import ru.hh.jclient.common.metric.MetricProvider;
import ru.hh.jclient.common.util.storage.Storage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

//TODO rename to HttpClientFactory
public class HttpClientBuilder {

  private final AsyncHttpClient http;
  private final Set<String> hostsWithSession;
  private final Storage<HttpClientContext> contextSupplier;
  private final Executor callbackExecutor;
  private final UpstreamManager upstreamManager;

  public HttpClientBuilder(AsyncHttpClient http, Collection<String> hostsWithSession, Storage<HttpClientContext> contextSupplier) {
    this(http, hostsWithSession, contextSupplier, Runnable::run);
  }

  public HttpClientBuilder(AsyncHttpClient http,
                           Collection<String> hostsWithSession,
                           Storage<HttpClientContext> contextSupplier,
                           Executor callbackExecutor) {
    this(http, hostsWithSession, contextSupplier, callbackExecutor, new DefaultUpstreamManager());
  }

  public HttpClientBuilder(AsyncHttpClient http,
                           Collection<String> hostsWithSession,
                           Storage<HttpClientContext> contextSupplier,
                           Executor callbackExecutor,
                           UpstreamManager upstreamManager) {
    this.http = requireNonNull(http, "http must not be null");
    this.hostsWithSession = ImmutableSet.copyOf(requireNonNull(hostsWithSession, "hostsWithSession must not be null"));
    this.contextSupplier = requireNonNull(contextSupplier, "contextSupplier must not be null");
    this.callbackExecutor = requireNonNull(callbackExecutor, "callbackExecutor must not be null");
    this.upstreamManager = requireNonNull(upstreamManager, "upstreamManager must not be null");
  }

  /**
   * Specifies request to be executed. This is a starting point of request execution chain.
   *
   * @param request
   *          to execute
   *
   * @deprecated use {@link #with(Request)}
   */
  @Deprecated
  public HttpClient with(com.ning.http.client.Request request) {
    return new HttpClientImpl(
        http,
        new Request(requireNonNull(request, "request must not be null")),
        hostsWithSession,
        upstreamManager,
        contextSupplier,
        callbackExecutor);
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
        callbackExecutor);
  }

  /**
   * Same as {@link #with(Request request)}, but is going to be adaptively balanced over available upstreams
   *
   * @param request
   *          to execute
   */
  public HttpClient withAdaptive(Request request) {
    return new HttpClientImpl(
        http,
        requireNonNull(request, "request must not be null"),
        hostsWithSession,
        upstreamManager,
        contextSupplier,
        callbackExecutor,
        true);
  }

  /**
   * @return returns copy (within case insensitive map) of headers contained within global (incoming) request
   */
  public Map<String, List<String>> getHeaders() {
    return new FluentCaseInsensitiveStringsMap(contextSupplier.get().getHeaders());
  }

  AsyncHttpClient getHttp() {
    return http;
  }

  MetricProvider getMetricProvider(boolean provideExtendedMetrics) {
    return MetricProviderFactory.from(this, provideExtendedMetrics);
  }

}
