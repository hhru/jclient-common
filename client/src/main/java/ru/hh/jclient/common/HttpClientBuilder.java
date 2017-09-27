package ru.hh.jclient.common;

import com.google.common.collect.ImmutableSet;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Request;
import static java.util.Objects.requireNonNull;
import ru.hh.jclient.common.balancing.UpstreamManager;
import ru.hh.jclient.common.util.storage.Storage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

public class HttpClientBuilder {

  private final AsyncHttpClient http;
  private final Set<String> hostsWithSession;
  private final Storage<HttpClientContext> contextSupplier;
  private final Executor callbackExecutor;
  private final UpstreamManager upstreamManager;

  public HttpClientBuilder(AsyncHttpClient http, Collection<String> hostsWithSession, Storage<HttpClientContext> contextSupplier) {
    this(http, hostsWithSession, contextSupplier, Runnable::run, Collections.emptyMap(), 0);
  }

  public HttpClientBuilder(AsyncHttpClient http,
                           Collection<String> hostsWithSession,
                           Storage<HttpClientContext> contextSupplier,
                           Executor callbackExecutor) {
    this(http, hostsWithSession, contextSupplier, callbackExecutor, Collections.emptyMap(), 0);
  }

  public HttpClientBuilder(AsyncHttpClient http,
                           Collection<String> hostsWithSession,
                           Storage<HttpClientContext> contextSupplier,
                           Executor callbackExecutor,
                           Map<String, String> upstreamConfigs,
                           int logStatsIntervalMs) {
    this.http = requireNonNull(http, "http must not be null");
    this.hostsWithSession = ImmutableSet.copyOf(requireNonNull(hostsWithSession, "hostsWithSession must not be null"));
    this.contextSupplier = requireNonNull(contextSupplier, "contextSupplier must not be null");
    this.callbackExecutor = requireNonNull(callbackExecutor, "callbackExecutor must not be null");
    upstreamManager = new UpstreamManager(requireNonNull(upstreamConfigs, "upstream configs should not be null"), logStatsIntervalMs);
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
        hostsWithSession,
        upstreamManager,
        contextSupplier,
        callbackExecutor);
  }

  /**
   * @return returns copy (within case insensitive map) of headers contained within global (incoming) request
   */
  public Map<String, List<String>> getHeaders() {
    return new FluentCaseInsensitiveStringsMap(contextSupplier.get().getHeaders());
  }

  public void updateUpstream(String name, String configString) {
    upstreamManager.updateUpstream(name, configString);
  }

  UpstreamManager getUpstreamManager() {
    return upstreamManager;
  }
}
