package ru.hh.jclient.common;

import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.xml.bind.JAXBContext;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
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

  public <T> AbstractProcessor<T> expectXml(JAXBContext context, Class<T> xmlClass) {
    return new XmlProcessor<>(this, context, xmlClass);
  }

  public <T> AbstractProcessor<T> expectJson(ObjectMapper mapper, Class<T> jsonClass) {
    return new JsonProcessor<>(this, mapper, jsonClass);
  }

  public <T extends GeneratedMessage> AbstractProcessor<T> expectProtobuf(Class<T> protobufClass) {
    return new ProtobufProcessor<>(this, protobufClass);
  }

  public AbstractProcessor<String> expectPlainText() {
    return new PlainTextProcessor(this);
  }

  public AbstractProcessor<String> expectPlainText(Charset charset) {
    return new PlainTextProcessor(this, charset);
  }

  public AbstractProcessor<Void> expectEmpty() {
    return new VoidProcessor(this);
  }

  abstract <T> CompletableFuture<ResponseWrapper<T>> executeRequest(FailableFunction<Response, ResponseWrapper<T>, Exception> converter);

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
