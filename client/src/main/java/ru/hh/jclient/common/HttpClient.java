package ru.hh.jclient.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.MessageLite;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.util.function.Supplier;
import org.asynchttpclient.AsyncHttpClient;
import ru.hh.jclient.common.responseconverter.JsonCollectionConverter;
import ru.hh.jclient.common.responseconverter.JsonConverter;
import ru.hh.jclient.common.responseconverter.JsonMapConverter;
import ru.hh.jclient.common.responseconverter.PlainTextConverter;
import ru.hh.jclient.common.responseconverter.ProtobufConverter;
import ru.hh.jclient.common.responseconverter.TypeConverter;
import ru.hh.jclient.common.responseconverter.VoidConverter;
import ru.hh.jclient.common.responseconverter.XmlConverter;
import ru.hh.jclient.common.util.SimpleRange;
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
  public static final SimpleRange OK_RANGE = new SimpleRange(0, 399);
  public static final Function<Response, Boolean> OK_RESPONSE = r -> OK_RANGE.contains(r.getStatusCode());

  private final AsyncHttpClient http;
  private final Set<String> hostsWithSession;
  private final HttpClientContext context;
  private final Storages storages;
  private final RequestEngineBuilder requestEngineBuilder;
  private final List<HttpClientEventListener> eventListeners;

  private List<RequestDebug> debugs;
  private Request request;
  private Optional<?> requestBodyEntity = Optional.empty();
  private Optional<Collection<String>> expectedMediaTypes = Optional.empty();
  private Optional<Collection<String>> expectedMediaTypesForErrors = Optional.empty();

  private boolean readOnlyReplica;
  private boolean noSession;
  private boolean noDebug;
  private boolean externalRequest;

  HttpClient(AsyncHttpClient http,
             Request request,
             Set<String> hostsWithSession,
             RequestStrategy<? extends RequestEngineBuilder> requestStrategy,
             Storage<HttpClientContext> contextSupplier,
             List<HttpClientEventListener> eventListeners) {
    this.http = http;
    this.request = request;
    this.hostsWithSession = hostsWithSession;
    this.requestEngineBuilder = requestStrategy.createRequestEngineBuilder(this);
    this.eventListeners = eventListeners;

    context = Optional.ofNullable(contextSupplier)
        .map(Supplier::get)
        .orElseThrow(() -> new RuntimeException(
            "Context for HttpClient is not provided. Usually this happens when a) jclient is called from a thread that has no context - " +
                "provide context, i.e. using HttpClientContextThreadLocalSupplier.addContext(...) / .clear() methods; " +
                " b) HttpClientFactoryBuilder was not created properly - ensure HttpClientContextThreadLocalSupplier is provided at creation time;" +
                " c) HttpClientContextThreadLocalSupplier.addContext(...) is not being called from corresponding (servlet) filter during " +
                " request handling - ensure proper filter is set up correctly."));

    storages = context.getStorages().copy().add(contextSupplier);
    debugs = context.getDebugSuppliers().stream().map(Supplier::get).collect(toList());
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

  private static final VoidConverter VOID_CONVERTER = new VoidConverter();

  /**
   * Specifies that the result must not be parsed.
   */
  public EmptyResultProcessor expectNoContent() {
    return new EmptyResultProcessor(this, VOID_CONVERTER);
  }

  /**
   * Specifies that the result must not be parsed.
   *
   * @deprecated use {@link #expectNoContent()}
   */
  @Deprecated // use #expectNoContent()
  public ResultProcessor<Void> expectEmpty() {
    return new ResultProcessor<>(this, VOID_CONVERTER);
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
   * Entrypoint to configure engine-specific properties of the client
   * @param clazz specific implementation type of {@link RequestEngineBuilder}
   * @return requestEngineBuilder casted to specific type
   */
  public <T extends RequestEngineBuilder> T configureRequestEngine(Class<T> clazz) {
    return (T) requestEngineBuilder;
  }

  /**
   * Returns unconverted, raw response. Avoid using this method, use "converter" methods instead.
   *
   * @return response
   */
  public CompletableFuture<Response> unconverted() {
    RequestStrategy.RequestExecutor requestExecutor = new RequestStrategy.RequestExecutor() {
      @Override
      public CompletableFuture<ResponseWrapper> executeRequest(Request request, int retryCount, RequestContext requestContext) {
        if (retryCount > 0) {
          // due to retry possibly performed in another thread
          // TODO do not re-get suppliers here
          debugs = context.getDebugSuppliers().stream().map(Supplier::get).collect(toList());
        }
        return HttpClient.this.executeRequest(request, retryCount, requestContext);
      }

      @Override
      public int getDefaultRequestTimeoutMs() {
        return http.getConfig().getRequestTimeout();
      }
    };
    return requestEngineBuilder.build(request, requestExecutor).execute();
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

  // set within ru.hh.nab.jclient.JClientContextProviderFilter
  public HttpClientContext getContext() {
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

  Optional<Collection<String>> getExpectedMediaTypes() {
    return expectedMediaTypes;
  }

  Optional<Collection<String>> getExpectedMediaTypesForErrors() {
    return expectedMediaTypesForErrors;
  }

  void setExpectedMediaTypesForErrors(Optional<Collection<String>> mediaTypes) {
    expectedMediaTypesForErrors = mediaTypes;
  }

  List<RequestDebug> getDebugs() {
    return debugs;
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
