package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Request;
import ru.hh.jclient.common.util.storage.Storage;

public class HttpClientBuilder {

  private AsyncHttpClient http;
  private Set<String> hostsWithSession;
  private Storage<HttpClientContext> contextSupplier;

  public HttpClientBuilder(AsyncHttpClient http, Collection<String> hostsWithSession, Storage<HttpClientContext> contextSupplier) {
    this.http = requireNonNull(http, "http must not be null");
    this.hostsWithSession = ImmutableSet.copyOf(requireNonNull(hostsWithSession, "hostsWithSession must not be null"));
    this.contextSupplier = requireNonNull(contextSupplier, "contextSupplier must not be null");
  }

  /**
   * Specifies request to be executed. This is a starting point of request execution chain.
   *
   * @param request to execute
   */
  public HttpClient with(Request request) {
    return new HttpClientImpl(http, requireNonNull(request, "request must not be null"), hostsWithSession, contextSupplier);
  }

  /**
   * @return returns copy (within case insensitive map) of headers contained within global (incoming) request
   */
  public Map<String, List<String>> getHeaders() {
    return new FluentCaseInsensitiveStringsMap(contextSupplier.get().getHeaders());
  }

}
