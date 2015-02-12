package ru.hh.jclient.common;

import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.xml.bind.JAXBContext;
import ru.hh.jclient.common.converter.JsonConverter;
import ru.hh.jclient.common.converter.PlainTextConverter;
import ru.hh.jclient.common.converter.ProtobufConverter;
import ru.hh.jclient.common.converter.TypeConverter;
import ru.hh.jclient.common.converter.VoidConverter;
import ru.hh.jclient.common.converter.XmlConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Range;
import com.google.protobuf.GeneratedMessage;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

public abstract class HttpClient {

  public static final Range<Integer> OK_RANGE = Range.atMost(399);
  public static final Function<Response, Boolean> OK_RESPONSE = r -> OK_RANGE.contains(r.getStatusCode());

  private AsyncHttpClient http;
  private Set<String> hostsWithSession;
  private HttpClientContext context;
  private RequestDebug debug;

  private Request request;

  private boolean readOnlyReplica;

  HttpClient(AsyncHttpClient http, Request request, Set<String> hostsWithSession, Supplier<HttpClientContext> contextSupplier) {
    this.http = http;
    this.request = request;
    this.hostsWithSession = hostsWithSession;
    this.context = contextSupplier.get();
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

  // parsing response

  /**
   * Specifies that the type of result must be XML.
   * 
   * @param context JAXB context used to parse response
   * @param xmlClass type of result
   */
  public <T> ResultProcessor<T> expectXml(JAXBContext context, Class<T> xmlClass) {
    return new ResultProcessor<T>(this, new XmlConverter<>(context, xmlClass));
  }

  /**
   * Specifies that the type of result must be JSON.
   * 
   * @param mapper Jackson mapper used to parse response
   * @param jsonClass type of result
   */
  public <T> ResultProcessor<T> expectJson(ObjectMapper mapper, Class<T> jsonClass) {
    return new ResultProcessor<T>(this, new JsonConverter<>(mapper, jsonClass));
  }

  /**
   * Specifies that the type of result must be PROTOBUF.
   * 
   * @param protobufClass type of result
   */
  public <T extends GeneratedMessage> ResultProcessor<T> expectProtobuf(Class<T> protobufClass) {
    return new ResultProcessor<T>(this, new ProtobufConverter<>(protobufClass));
  }

  /**
   * Specifies that the type of result must be plain text with {@link PlainTextConverter#DEFAULT default} encoding.
   */
  public ResultProcessor<String> expectPlainText() {
    return new ResultProcessor<String>(this, new PlainTextConverter());
  }

  /**
   * Specifies that the type of result must be plain text.
   * 
   * @param charset used to decode response
   */
  public ResultProcessor<String> expectPlainText(Charset charset) {
    return new ResultProcessor<String>(this, new PlainTextConverter(charset));
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

  RequestDebug getDebug() {
    return debug;
  }

  boolean useReadOnlyReplica() {
    return readOnlyReplica;
  }
}
