package ru.hh.jclient.common;

import java.util.Set;
import java.util.function.Supplier;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.RequestBuilder;

public class HttpClientBuilder {

  private AsyncHttpClient http;
  private Set<String> hostsWithSession;
  private Supplier<HttpRequestContext> contextSupplier;

  public HttpClientBuilder(AsyncHttpClient http, Set<String> hostsWithSession, Supplier<HttpRequestContext> contextSupplier) {
    this.http = http;
    this.hostsWithSession = hostsWithSession;
    this.contextSupplier = contextSupplier;
  }

  public HttpClient with(RequestBuilder builder) {
    return new HttpClient(http, contextSupplier, hostsWithSession, builder);
  }

}
