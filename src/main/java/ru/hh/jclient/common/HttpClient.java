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

  private AsyncHttpClient http;
  private Set<String> hostsWithSession;
  private HttpClientContext context;
  private RequestDebug debug;

  private Request request;

  // tools for response parsing
  private ReturnType returnType;
  private JAXBContext jaxbContext;
  private Class<? extends GeneratedMessage> protobufClass;
  private ObjectMapper objectMapper;
  private Class<?> jsonClass;

  private boolean readOnlyReplica;

  HttpClient(AsyncHttpClient http, Request request, Set<String> hostsWithSession, Supplier<HttpClientContext> contextSupplier) {
    this.http = http;
    this.request = request;
    this.hostsWithSession = hostsWithSession;
    this.context = contextSupplier.get();
    this.debug = this.context.getDebugSupplier().get();
  }

  public HttpClient readOnly() {
    this.readOnlyReplica = true;
    this.debug.addLabel("RO");
    return this;
  }

  // parsing response

  public <T> CompletableFuture<T> returnXml(JAXBContext context) {
    this.returnType = ReturnType.XML;
    this.jaxbContext = context;
    return executeRequest();
  }

  public <T> CompletableFuture<T> returnJson(ObjectMapper mapper, Class<?> jsonClass) {
    this.returnType = ReturnType.JSON;
    this.objectMapper = mapper;
    this.jsonClass = jsonClass;
    return executeRequest();
  }

  public <T> CompletableFuture<T> returnProtobuf(Class<? extends GeneratedMessage> protobufClass) {
    this.returnType = ReturnType.PROTOBUF;
    this.protobufClass = protobufClass;
    return executeRequest();
  }

  public <T> CompletableFuture<T> returnText() {
    this.returnType = ReturnType.TEXT;
    return executeRequest();
  }

  public <T> CompletableFuture<T> returnEmpty() {
    this.returnType = ReturnType.EMPTY;
    return executeRequest();
  }

  abstract <T> CompletableFuture<T> executeRequest();

  // getters for tools

  AsyncHttpClient getHttp() {
    return http;
  }

  Request getRequest() {
    return request;
  }

  Set<String> getHostsWithSession() {
    return hostsWithSession;
  }

  HttpClientContext getContext() {
    return context;
  }

  RequestDebug getDebug() {
    return debug;
  }

  boolean useReadOnlyReplica() {
    return readOnlyReplica;
  }

  // getters for response generation tools

  ReturnType getReturnType() {
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


}
