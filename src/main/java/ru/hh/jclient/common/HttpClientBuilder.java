package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableSet;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;

public class HttpClientBuilder {

  private AsyncHttpClient http;
  private Set<String> hostsWithSession;
  private Supplier<HttpClientContext> contextSupplier;

  public HttpClientBuilder(AsyncHttpClient http, Collection<String> hostsWithSession, Supplier<HttpClientContext> contextSupplier) {
    this.http = requireNonNull(http, "http must not be null");
    this.hostsWithSession = ImmutableSet.copyOf(requireNonNull(hostsWithSession, "hostsWithSession must not be null"));
    this.contextSupplier = requireNonNull(contextSupplier, "contextSupplier must not be null");
  }

  /**
   * Provides request to be executed.
   * 
   * @param request to execute
   */
  public HttpClient with(Request request) {
    return new HttpClientImpl(http, requireNonNull(request, "request must not be null"), hostsWithSession, contextSupplier);
  }

}
