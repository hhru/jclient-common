package ru.hh.jclient.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Range;
import com.google.common.net.MediaType;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.MessageLite;
import static java.util.Objects.requireNonNull;

import org.asynchttpclient.AsyncHttpClient;
import ru.hh.jclient.common.balancing.RequestBalancer;
import ru.hh.jclient.common.balancing.RequestBalancer.RequestExecutor;
import ru.hh.jclient.common.responseconverter.JavaSerializedConverter;
import ru.hh.jclient.common.responseconverter.JsonCollectionConverter;
import ru.hh.jclient.common.responseconverter.JsonConverter;
import ru.hh.jclient.common.responseconverter.JsonMapConverter;
import ru.hh.jclient.common.responseconverter.PlainTextConverter;
import ru.hh.jclient.common.responseconverter.ProtobufConverter;
import ru.hh.jclient.common.responseconverter.TypeConverter;
import ru.hh.jclient.common.responseconverter.VoidConverter;
import ru.hh.jclient.common.responseconverter.XmlConverter;
import ru.hh.jclient.common.util.storage.Storage;
import ru.hh.jclient.common.util.storage.StorageUtils.Storages;

import javax.xml.bind.JAXBContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class HttpClient {
  public static final Range<Integer> OK_RANGE = Range.atMost(399);
  public static final Function<Response, Boolean> OK_RESPONSE = r -> OK_RANGE.contains(r.getStatusCode());

  private final AsyncHttpClient http;
  private final Set<String> hostsWithSession;
  private final HttpClientContext context;
  private final Storages storages;
  private final UpstreamManager upstreamManager;
  private final List<HttpClientEventListener> eventListeners;

  private RequestDebug debug;
  private Request request;
  private Optional<?> requestBodyEntity = Optional.empty();
  private Optional<Collection<MediaType>> expectedMediaTypes = Optional.empty();
  private Integer maxRequestTimeoutTries;
  private boolean forceIdempotence = false;

  private boolean readOnlyReplica;
  private boolean noSession;
  private boolean noDebug;
  private boolean externalRequest;
  private boolean adaptive;
  private String dynamicUpstreamKey;

  HttpClient(AsyncHttpClient http,
             Request request,
             Set<String> hostsWithSession,
             UpstreamManager upstreamManager,
             Storage<HttpClientContext> contextSupplier,
             List<HttpClientEventListener> eventListeners,
             boolean adaptive) {
    this.http = http;
    this.request = request;
    this.hostsWithSession = hostsWithSession;
    this.upstreamManager = upstreamManager;
    this.adaptive = adaptive;
    this.eventListeners = eventListeners;

    context = contextSupplier.get();
    storages = context.getStorages().copy().add(contextSupplier);
    debug = context.getDebugSupplier().get();
  }

  /**
   * Marks request as "read only". Adds corresponding GET attribute to request url.
   */
  public HttpClient readOnly() {
    readOnlyReplica = true;
    return this;
  }

  /**
   * Forces client NOT to send {@link HttpHeaderNames#HH_PROTO_SESSION} header.
   */
  public HttpClient noSession() {
    noSession = true;
    return this;
  }

  /**
   * Tells client the request will be performed to external resource. Client will not pass-through any of {@link HttpClientImpl#PASS_THROUGH_HEADERS}.
   */
  public HttpClient external() {
    externalRequest = true;
    return this;
  }

  /**
   * Forces client NOT to send {@link HttpHeaderNames#X_HH_DEBUG} header.
   */
  public HttpClient noDebug() {
    noDebug = true;
    return this;
  }

  /**
   * Use adaptive balancing.
   */
  public HttpClient adaptive() {
    adaptive = true;
    return this;
  }

  /**
   * Sets dynamic upstream key to allow flexible upstream selection.
   */
  public HttpClient setDynamicUpstreamKey(String key) {
    dynamicUpstreamKey = key;
    return this;
  }

  /**
   * Overrides {@link ru.hh.jclient.common.balancing.UpstreamConfig#getMaxTimeoutTries() when using {@link RequestBalancer}}
   */
  public HttpClient withMaxRequestTimeoutTries(Integer maxRequestTimeoutTries) {
    this.maxRequestTimeoutTries = maxRequestTimeoutTries;
    return this;
  }

  /**
   * Force request idempotence, default false
   */
  public HttpClient forceIdempotence(boolean forceIdempotence) {
    this.forceIdempotence = forceIdempotence;
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
    requestBodyEntity = Optional.of(requireNonNull(body, "body must not be null"));
    RequestBuilder builder = new RequestBuilder(request);
    builder.setBody(body.toByteArray(), "application/x-protobuf");
    request = builder.build();
    return this;
  }

  /**
   * Convenience method that sets java object as request body as well as corresponding "Content-type" header. Provided object will be used in
   * debug output of request in debug mode.
   *
   * @param body
   *          java object to send in request
   */
  public HttpClient withJavaObjectBody(Object body) {
    requestBodyEntity = Optional.of(requireNonNull(body, "body must not be null"));
    RequestBuilder builder = new RequestBuilder(request);
    try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
         ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
      out.writeObject(body);
      builder.setBody(byteOut.toByteArray(), "application/x-java-serialized-object");
    } catch (IOException e) {
      throw new RuntimeException("failed to write java object", e);
    }
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
   * Specifies that the type of result must be a collection of JSON objects.
   *
   * @param mapper Jackson mapper used to parse response
   * @param jsonClass type of JSON object allowing generics
   */
  public <T> ResultProcessor<Collection<T>> expectJsonCollection(ObjectMapper mapper, TypeReference<T> jsonClass) {
    TypeConverter<Collection<T>> converter = new JsonCollectionConverter<>(mapper, jsonClass);
    expectedMediaTypes = converter.getSupportedMediaTypes();
    return new ResultProcessor<>(this, converter);
  }

  /**
   * Specifies that the type of result must be a map with JSON objects.
   *
   * @param mapper Jackson mapper used to parse response
   * @param jsonKeyClass type of Key object (works only with simple types: String, Integer, etc)
   * @param jsonValueClass type of Value object
   */
  public <K, V> ResultProcessor<Map<K, V>> expectJsonMap(ObjectMapper mapper, Class<K> jsonKeyClass, Class<V> jsonValueClass) {
    TypeConverter<Map<K, V>> converter = new JsonMapConverter<>(mapper, jsonKeyClass, jsonValueClass);
    expectedMediaTypes = converter.getSupportedMediaTypes();
    return new ResultProcessor<>(this, converter);
  }

  /**
   * Specifies that the type of result must be a map with JSON objects.
   *
   * @param mapper Jackson mapper used to parse response
   * @param jsonKeyClass type of Key object (works only with simple types: String, Integer, etc)
   * @param jsonValueClass type of Value object allowing generic specification
   */
  public <K, V> ResultProcessor<Map<K, V>> expectJsonMap(ObjectMapper mapper, Class<K> jsonKeyClass, TypeReference<V> jsonValueClass) {
    TypeConverter<Map<K, V>> converter = new JsonMapConverter<>(mapper, jsonKeyClass, jsonValueClass);
    expectedMediaTypes = converter.getSupportedMediaTypes();
    return new ResultProcessor<>(this, converter);
  }

  /**
   * Specifies that the type of result must be PROTOBUF.
   *
   * @param protobufClass type of result
   */
  public <T extends GeneratedMessageV3> ResultProcessor<T> expectProtobuf(Class<T> protobufClass) {
    TypeConverter<T> converter = new ProtobufConverter<>(protobufClass);
    expectedMediaTypes = converter.getSupportedMediaTypes();
    return new ResultProcessor<>(this, converter);
  }

  /**
   * Specifies that the type of result must be serialized clazz or derivatives.
   */
  public <T> ResultProcessor<T> expectJavaSerialized(Class<T> clazz) {
    TypeConverter<T> converter = new JavaSerializedConverter<>(clazz);
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
    return new ResultProcessor<>(this, new VoidConverter());
  }

  /**
   * Specifies the converter for the result.
   *
   * @param converter used to convert response to expected result
   */
  public <T> ResultProcessor<T> expect(TypeConverter<T> converter) {
    expectedMediaTypes = converter.getSupportedMediaTypes();
    return new ResultProcessor<>(this, converter);
  }

  /**
   * Returns unconverted, raw response. Avoid using this method, use "converter" methods instead.
   *
   * @return response
   */
  public CompletableFuture<Response> unconverted() {
    RequestExecutor requestExecutor = (request, retryCount, requestContext) -> {
      if (retryCount > 0) {
        debug = context.getDebugSupplier().get();
      }
      return executeRequest(request, retryCount, requestContext);
    };
    RequestBalancer requestBalancer = new RequestBalancer(request, upstreamManager, requestExecutor,
      maxRequestTimeoutTries, forceIdempotence, adaptive, dynamicUpstreamKey);
    return requestBalancer.requestWithRetry();
  }

  abstract CompletableFuture<ResponseWrapper> executeRequest(Request request, int retryCount, RequestContext context);

  boolean isNoSessionRequired() {
    String host = request.getUri().getHost();
    return noSession || hostsWithSession.stream().map(Uri::create).map(Uri::getHost).noneMatch(host::equals);
  }

  // getters for tools

  AsyncHttpClient getHttp() {
    return http;
  }

  HttpClientContext getContext() {
    return context;
  }

  Storages getStorages() {
    return storages;
  }

  List<HttpClientEventListener> getEventListeners() {
    return eventListeners;
  }

  Optional<?> getRequestBodyEntity() {
    return requestBodyEntity;
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

  boolean isExternalRequest() {
    return externalRequest;
  }

  boolean isNoDebug() {
    return noDebug;
  }
}
