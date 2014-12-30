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
  private Supplier<HttpRequestContext> contextSupplier;
  private Supplier<HttpRequestInfo> infoSupplier;

  public HttpClientBuilder(AsyncHttpClient http, Collection<String> hostsWithSession, Supplier<HttpRequestContext> contextSupplier,
      Supplier<HttpRequestInfo> infoSupplier) {
    this.http = http;
    this.hostsWithSession = new HashSet<>(hostsWithSession);
    this.contextSupplier = contextSupplier;
    this.infoSupplier = infoSupplier;
  }

  public HttpClient with(Request request) {
    return new HttpClientImpl(http, request, hostsWithSession, contextSupplier, infoSupplier);
  }

}
