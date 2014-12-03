package ru.hh.jclient.common;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.xml.bind.JAXBContext;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.RequestBuilder;

public class HttpClient {

  private AsyncHttpClient http;
  private Set<String> hostsWithSession;
  private HttpRequestContext context;

  private RequestBuilder requestBuilder;
  private HttpRequestReturnType returnType;
  private JAXBContext jaxbContext;

  HttpClient(AsyncHttpClient http, Supplier<HttpRequestContext> contextSupplier, Set<String> hostsWithSession, RequestBuilder requestBuilder) {
    this.http = http;
    this.context = contextSupplier.get();
    this.hostsWithSession = hostsWithSession;
    this.requestBuilder = requestBuilder;
  }

  private HttpRequestExecutor getExecutor() {
    return new HttpRequestExecutor(http, this);
  }

  public <T> CompletableFuture<T> returnXml(JAXBContext context) {
    this.returnType = HttpRequestReturnType.XML;
    this.jaxbContext = context;
    return getExecutor().request(this);
  }

  public <T> CompletableFuture<T> returnEmpty() {
    this.returnType = HttpRequestReturnType.EMPTY;
    return getExecutor().request(this);
  }

  public Set<String> getHostsWithSession() {
    return hostsWithSession;
  }

  HttpRequestContext getContext() {
    return context;
  }

  RequestBuilder getRequestBuilder() {
    return requestBuilder;
  }

  HttpRequestReturnType getReturnType() {
    return returnType;
  }

  JAXBContext getJaxbContext() {
    return jaxbContext;
  }
}
