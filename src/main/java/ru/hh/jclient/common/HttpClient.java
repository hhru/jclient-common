package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.xml.bind.JAXBContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.GeneratedMessage;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

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
  private Charset charset;

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
    CompletableFuture<ResponseWrapper<T>> future = returnWrappedXml(context);
    return future.thenApply(ResponseWrapper::get);
  }

  public <T> CompletableFuture<ResponseWrapper<T>> returnWrappedXml(JAXBContext context) {
    this.returnType = ReturnType.XML;
    this.jaxbContext = requireNonNull(context, "context must not be null");
    return executeRequest();
  }

  public <T> CompletableFuture<T> returnJson(ObjectMapper mapper, Class<T> jsonClass) {
    CompletableFuture<ResponseWrapper<T>> future = returnWrappedJson(mapper, jsonClass);
    return future.thenApply(ResponseWrapper::get);
  }

  public <T> CompletableFuture<ResponseWrapper<T>> returnWrappedJson(ObjectMapper mapper, Class<T> jsonClass) {
    this.returnType = ReturnType.JSON;
    this.objectMapper = requireNonNull(mapper, "mapper must not be null");
    this.jsonClass = requireNonNull(jsonClass, "jsonClass must not be null");
    return executeRequest();
  }

  public <T extends GeneratedMessage> CompletableFuture<T> returnProtobuf(Class<T> protobufClass) {
    CompletableFuture<ResponseWrapper<T>> future = returnWrappedProtobuf(protobufClass);
    return future.thenApply(ResponseWrapper::get);
  }

  public <T extends GeneratedMessage> CompletableFuture<ResponseWrapper<T>> returnWrappedProtobuf(Class<T> protobufClass) {
    this.returnType = ReturnType.PROTOBUF;
    this.protobufClass = requireNonNull(protobufClass, "protobufClass must not be null");
    return executeRequest();
  }

  public CompletableFuture<String> returnText() {
    return returnText(StandardCharsets.UTF_8);
  }

  public CompletableFuture<String> returnText(Charset charset) {
    CompletableFuture<ResponseWrapper<String>> future = returnWrappedText(charset);
    return future.thenApply(ResponseWrapper::get);
  }

  public CompletableFuture<ResponseWrapper<String>> returnWrappedText() {
    return returnWrappedText(StandardCharsets.UTF_8);
  }

  public CompletableFuture<ResponseWrapper<String>> returnWrappedText(Charset charset) {
    this.returnType = ReturnType.TEXT;
    this.charset = requireNonNull(charset, "charset must not be null");
    return executeRequest();
  }

  public <T> CompletableFuture<Void> returnEmpty() {
    this.returnType = ReturnType.EMPTY;
    return executeRequest().thenApply(rw -> null);
  }

  public CompletableFuture<Response> returnResponse() {
    this.returnType = ReturnType.EMPTY;
    return executeRequest().thenApply(ResponseWrapper::getResponse);
  }

  abstract <T> CompletableFuture<ResponseWrapper<T>> executeRequest();

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

  Charset getCharset() {
    return charset;
  }
}
