package ru.hh.jclient.common;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.xml.bind.JAXBContext;

import ru.hh.jclient.common.converter.JsonCollectionConverter;
import ru.hh.jclient.common.converter.JsonConverter;
import ru.hh.jclient.common.converter.PlainTextConverter;
import ru.hh.jclient.common.converter.ProtobufConverter;
import ru.hh.jclient.common.converter.TypeConverter;
import ru.hh.jclient.common.converter.VoidConverter;
import ru.hh.jclient.common.converter.XmlConverter;
import ru.hh.jclient.common.util.storage.Storage;
import ru.hh.jclient.common.util.storage.StorageUtils.Storages;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Range;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.MessageLite;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;

public abstract class HttpClient {

  public static final Range<Integer> OK_RANGE = Range.atMost(399);
  public static final Function<Response, Boolean> OK_RESPONSE = r -> OK_RANGE.contains(r.getStatusCode());

  private AsyncHttpClient http;
  private Set<String> hostsWithSession;
  private HttpClientContext context;
  private Storages storages;
  private RequestDebug debug;

  protected Optional<?> requestBodyEntity = Optional.empty();
  private Optional<Collection<MediaType>> expectedMediaTypes = Optional.empty();

  private Request request;

  private boolean readOnlyReplica;
  private boolean noSession;
  private boolean noDebug;
  private boolean externalRequest;

  HttpClient(AsyncHttpClient http, Request request, Set<String> hostsWithSession, Storage<HttpClientContext> contextSupplier) {
    this.http = http;
    this.request = request;
    this.hostsWithSession = hostsWithSession;
    this.context = contextSupplier.get();
    this.storages = this.context.getStorages().copy().add(contextSupplier); //
    this.debug = this.context.getDebugSupplier().get();
  }

  /**
   * Marks request as "read only". Adds corresponding GET attribute to request url.
   */
  public HttpClient readOnly() {
    this.readOnlyReplica = true;
    this.debug.addLabel("RO");
    return this;
  }

  /**
   * Forces client NOT to send {@link ru.hh.jclient.common.HttpHeaders#HH_PROTO_SESSION} header.
   */
  public HttpClient noSession() {
    this.noSession = true;
    this.debug.addLabel("NOSESSION");
    return this;
  }

  /**
   * Tells client the request will be performed to external resource. Client will not pass-through any of {@link HttpClientImpl#PASS_THROUGH_HEADERS}.
   */
  public HttpClient external() {
    this.externalRequest = true;
    this.debug.addLabel("EXTERNAL");
    return this;
  }

  /**
   * Forces client NOT to send {@link ru.hh.jclient.common.HttpHeaders#X_HH_DEBUG} header.
   */
  public HttpClient noDebug() {
    this.noDebug = true;
    this.debug.addLabel("NODEBUG");
    return this;
  }

  /**
   * Convenience method that sets protobuf object as request body as well as corresponding "Content-type" header. Provided object will be used in
   * debug output of request in debug mode.
   *
   * @param body
   *          protobuf object to send in request
   */
  public HttpClient withProtobufBody(MessageLite body) {
    Objects.requireNonNull(body, "body must not be null");
    this.requestBodyEntity = Optional.of(body);
    RequestBuilder builder = new RequestBuilder(request);
    builder.setBody(body.toByteArray());
    builder.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-protobuf");
    request = builder.build();
    return this;
  }

  // parsing response

  /**
   * Specifies that the type of result must be XML.
   *
   * @param context JAXB context used to parse response
   * @param xmlClass type of result
   */
  public <T> ResultProcessor<T> expectXml(JAXBContext context, Class<T> xmlClass) {
    TypeConverter<T> converter = new XmlConverter<>(context, xmlClass);
    expectedMediaTypes = converter.getSupportedMediaTypes();
    return new ResultProcessor<>(this, converter);
  }

  /**
   * Specifies that the type of result must be JSON.
   *
   * @param mapper Jackson mapper used to parse response
   * @param jsonClass type of result
   */
  public <T> ResultProcessor<T> expectJson(ObjectMapper mapper, Class<T> jsonClass) {
    TypeConverter<T> converter = new JsonConverter<>(mapper, jsonClass);
    expectedMediaTypes = converter.getSupportedMediaTypes();
    return new ResultProcessor<>(this, converter);
  }

  /**
   * Specifies that the type of result must be a collection of JSON objects.
   *
   * @param mapper Jackson mapper used to parse response
   * @param jsonClass type of JSON object
   */
  public <T> ResultProcessor<Collection<T>> expectJsonCollection(ObjectMapper mapper, Class<T> jsonClass) {
    TypeConverter<Collection<T>> converter = new JsonCollectionConverter<>(mapper, jsonClass);
    expectedMediaTypes = converter.getSupportedMediaTypes();
    return new ResultProcessor<>(this, converter);
  }

  /**
   * Specifies that the type of result must be PROTOBUF.
   *
   * @param protobufClass type of result
   */
  public <T extends GeneratedMessage> ResultProcessor<T> expectProtobuf(Class<T> protobufClass) {
    TypeConverter<T> converter = new ProtobufConverter<>(protobufClass);
    expectedMediaTypes = converter.getSupportedMediaTypes();
    return new ResultProcessor<>(this, converter);
  }

  /**
   * Specifies that the type of result must be plain text with {@link PlainTextConverter#DEFAULT default} encoding.
   */
  public ResultProcessor<String> expectPlainText() {
    TypeConverter<String> converter = new PlainTextConverter();
    expectedMediaTypes = converter.getSupportedMediaTypes();
    return new ResultProcessor<>(this, converter);
  }

  /**
   * Specifies that the type of result must be plain text.
   *
   * @param charset used to decode response
   */
  public ResultProcessor<String> expectPlainText(Charset charset) {
    return new ResultProcessor<>(this, new PlainTextConverter(charset));
  }

  /**
   * Specifies that the result must not be parsed.
   */
  public ResultProcessor<Void> expectEmpty() {
    return new ResultProcessor<Void>(this, new VoidConverter());
  }

  /**
   * Specifies the converter for the result.
   *
   * @param converter used to convert response to expected result
   */
  public <T> ResultProcessor<T> expect(TypeConverter<T> converter) {
    return new ResultProcessor<T>(this, converter);
  }

  /**
   * Returns unconverted, raw response. Avoid using this method, use "converter" methods instead.
   *
   * @return response
   */
  public CompletableFuture<Response> request() {
    return executeRequest();
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

  Storages getStorages() {
    return storages;
  }

  Optional<Collection<MediaType>> getExpectedMediaTypes() {
    return expectedMediaTypes;
  }

  RequestDebug getDebug() {
    return debug;
  }

  boolean useReadOnlyReplica() {
    return readOnlyReplica;
  }

  boolean isExternal() {
    return externalRequest;
  }

  boolean isNoSession() {
    return noSession;
  }

  boolean isNoDebug() {
    return noDebug;
  }
}
