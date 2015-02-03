package ru.hh.jclient.common;

import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.xml.bind.JAXBContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.GeneratedMessage;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

public abstract class HttpClient {

  static final Function<Response, Boolean> OK_RESPONSE = r -> r.getStatusCode() < 400;

  private AsyncHttpClient http;
  private Set<String> hostsWithSession;
  private HttpClientContext context;
  private RequestDebug debug;

  private Request request;

  // tools for response parsing
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

  public <T> ResponseProcessor<T> expectXml(JAXBContext context, Class<T> xmlClass) {
    return new ResponseProcessor<T>(this, new XmlConverter<>(context, xmlClass));
  }

  public <T> ResponseProcessor<T> expectJson(ObjectMapper mapper, Class<T> jsonClass) {
    return new ResponseProcessor<T>(this, new JsonConverter<>(mapper, jsonClass));
  }

  public <T extends GeneratedMessage> ResponseProcessor<T> expectProtobuf(Class<T> protobufClass) {
    return new ResponseProcessor<T>(this, new ProtobufConverter<>(protobufClass));
  }

  public ResponseProcessor<String> expectPlainText() {
    return new ResponseProcessor<String>(this, new PlainTextConverter());
  }

  public ResponseProcessor<String> expectPlainText(Charset charset) {
    return new ResponseProcessor<String>(this, new PlainTextConverter(charset));
  }

  public ResponseProcessor<Void> expectEmpty() {
    return new ResponseProcessor<Void>(this, new VoidConverter());
  }

  abstract CompletableFuture<Response> executeRequest();

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
