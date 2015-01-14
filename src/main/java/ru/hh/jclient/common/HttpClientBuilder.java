package ru.hh.jclient.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;

public class HttpClientBuilder {

  private AsyncHttpClient http;
  private Set<String> hostsWithSession;
  private Supplier<HttpClientContext> contextSupplier;

  public HttpClientBuilder(AsyncHttpClient http, Collection<String> hostsWithSession, Supplier<HttpClientContext> contextSupplier) {
    this.http = http;
    this.hostsWithSession = new HashSet<>(hostsWithSession);
    this.contextSupplier = contextSupplier;
  }

  public HttpClient with(Request request) {
    return new HttpClientImpl(http, request, hostsWithSession, contextSupplier);
  }

}
