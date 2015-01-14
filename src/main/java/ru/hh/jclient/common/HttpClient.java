package ru.hh.jclient.common;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.xml.bind.JAXBContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.GeneratedMessage;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;

public abstract class HttpClient {

  public static final String HEADER_DEBUG = "X-Hh-Debug";
  public static final String HEADER_SESSION = "Hh-Proto-Session";
  public static final String HEADER_AUTH = "Authorization";
  public static final String HEADER_REQUEST_ID = "X-Request-Id";
  public static final String HEADER_REAL_IP = "X-Real-IP";
  public static final String HEADER_COOKIE = "Cookie";

  private AsyncHttpClient http;
  private Set<String> hostsWithSession;
  private HttpRequestContext context;
  private HttpRequestInfo info;

  private Request request;
  private HttpRequestReturnType returnType;
  private JAXBContext jaxbContext;
  private Class<? extends GeneratedMessage> protobufClass;
  private ObjectMapper objectMapper;
  private Class<?> jsonClass;

  private boolean readOnlyReplica;

  HttpClient(AsyncHttpClient http, Request request, Set<String> hostsWithSession, Supplier<HttpRequestContext> contextSupplier,
      Supplier<HttpRequestInfo> infoSupplier) {
    this.http = http;
    this.request = request;
    this.hostsWithSession = hostsWithSession;
    this.context = contextSupplier.get();
    this.info = infoSupplier.get();
  }

  public HttpClient readOnly() {
    this.readOnlyReplica = true;
    this.info.addLabel("RO");
    return this;
  }

  public <T> CompletableFuture<T> returnXml(JAXBContext context) {
    this.returnType = HttpRequestReturnType.XML;
    this.jaxbContext = context;
    return executeRequest();
  }

  public <T> CompletableFuture<T> returnJson(ObjectMapper mapper, Class<?> jsonClass) {
    this.returnType = HttpRequestReturnType.JSON;
    this.objectMapper = mapper;
    this.jsonClass = jsonClass;
    return executeRequest();
  }

  public <T> CompletableFuture<T> returnProtobuf(Class<? extends GeneratedMessage> protobufClass) {
    this.returnType = HttpRequestReturnType.PROTOBUF;
    this.protobufClass = protobufClass;
    return executeRequest();
  }

  public <T> CompletableFuture<T> returnEmpty() {
    this.returnType = HttpRequestReturnType.EMPTY;
    return executeRequest();
  }

  abstract <T> CompletableFuture<T> executeRequest();

  AsyncHttpClient getHttp() {
    return http;
  }

  Request getRequest() {
    return request;
  }

  Set<String> getHostsWithSession() {
    return hostsWithSession;
  }

  HttpRequestContext getContext() {
    return context;
  }

  HttpRequestInfo getInfo() {
    return info;
  }

  HttpRequestReturnType getReturnType() {
    return returnType;
  }

  JAXBContext getJaxbContext() {
    return jaxbContext;
  }

  Class<? extends GeneratedMessage> getProtobufClass() {
    return protobufClass;
  }

  ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  Class<?> getJsonClass() {
    return jsonClass;
  }

  boolean useReadOnlyReplica() {
    return readOnlyReplica;
  }

}
