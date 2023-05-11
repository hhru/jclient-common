package ru.hh.jclient.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.GeneratedMessageV3;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.concurrent.CompletableFuture;
import javax.xml.bind.JAXBContext;
import ru.hh.jclient.common.exception.ClientResponseException;
import ru.hh.jclient.common.exception.ResponseConverterException;
import ru.hh.jclient.common.responseconverter.JsonCollectionConverter;
import ru.hh.jclient.common.responseconverter.JsonConverter;
import ru.hh.jclient.common.responseconverter.JsonMapConverter;
import ru.hh.jclient.common.responseconverter.PlainTextConverter;
import ru.hh.jclient.common.responseconverter.ProtobufConverter;
import ru.hh.jclient.common.responseconverter.TypeConverter;
import ru.hh.jclient.common.responseconverter.XmlConverter;

public class ResultProcessor<T> {

  private HttpClient httpClient;
  private TypeConverter<T> converter;

  ResultProcessor(HttpClient httpClient, TypeConverter<T> converter) {
    this.httpClient = requireNonNull(httpClient, "http client must not be null");
    this.converter = requireNonNull(converter, "converter must not be null");
    this.httpClient.getDebugs().forEach(d -> d.onCreateResultProcessor(httpClient, converter));
  }

  HttpClient getHttpClient() {
    return httpClient;
  }

  TypeConverter<T> getConverter() {
    return converter;
  }

  /**
   * Returns future containing expected result or {@link ClientResponseException}.
   *
   * @return expected result
   * @throws ClientResponseException if status code is not in {@link HttpClient#OK_RANGE}
   * @throws ResponseConverterException if converter failed to process response
   */
  public CompletableFuture<T> result() {
    return httpClient.unconverted().thenApply(this::wrapOrThrow).thenApply(rw -> rw.get().orElse(null));
  }

  /**
   * Returns future containing wrapper with response object and result (empty if status code is not in {@link HttpClient#OK_RANGE}).
   *
   * @return {@link ResultWithResponse} with expected result (possibly empty) and response object
   * @throws ResponseConverterException if converter failed to process response
   */
  public CompletableFuture<ResultWithResponse<T>> resultWithResponse() {
    return httpClient.unconverted().thenApply(this::wrapOrNull);
  }

  /**
   * Returns future containing wrapper with response status code and result (empty if status code is not in {@link HttpClient#OK_RANGE}).
   *
   * @return {@link ResultWithStatus} with expected result (possibly empty) and response status code
   * @throws ResponseConverterException if converter failed to process response
   */
  public CompletableFuture<ResultWithStatus<T>> resultWithStatus() {
    return resultWithResponse().thenApply(ResultWithResponse::hideResponse);
  }

  private ResultWithResponse<T> wrapOrThrow(Response response) {
    try {
      if (HttpClient.OK_RESPONSE.apply(response)) {
        return wrap(response);
      }
      throw new ClientResponseException(response);
    }
    finally {
      httpClient.getDebugs().forEach(RequestDebug::onProcessingFinished);
    }
  }

  private ResultWithResponse<T> wrapOrNull(Response response) {
    try {
      if (HttpClient.OK_RESPONSE.apply(response)) {
        return wrap(response);
      }
      return new ResultWithResponse<>(null, response);
    }
    finally {
      httpClient.getDebugs().forEach(RequestDebug::onProcessingFinished);
    }
  }

  private ResultWithResponse<T> wrap(Response response) {
    try {
      ResultWithResponse<T> result = converter.converterFunction().apply(response);
      httpClient.getDebugs().forEach(d -> d.onResponseConverted(result.get().orElse(null)));
      return result;
    }
    catch (ClientResponseException e) {
      throw e;
    }
    catch (Exception e) {
      ResponseConverterException rce = new ResponseConverterException("Failed to convert response", e);
      httpClient.getDebugs().forEach(d -> d.onConverterProblem(rce));
      throw rce;
    }
  }

  /**
   * Specifies that the type of ERROR result must be XML.
   *
   * @param context JAXB context used to parse response
   * @param xmlClass type of ERROR result
   */
  public <E> ResultOrErrorProcessor<T, E> orXmlError(JAXBContext context, Class<E> xmlClass) {
    XmlConverter<E> converter = new XmlConverter<>(context, xmlClass);
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new ResultOrErrorProcessor<>(this, converter);
  }

  /**
   * Specifies that the type of ERROR result must be JSON.
   *
   * @param mapper Jackson mapper used to parse response
   * @param jsonClass type of ERROR result
   */
  public <E> ResultOrErrorProcessor<T, E> orJsonError(ObjectMapper mapper, Class<E> jsonClass) {
    JsonConverter<E> converter = new JsonConverter<>(mapper, jsonClass);
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new ResultOrErrorProcessor<>(this, converter);
  }

  /**
   * Specifies that the type of ERROR result must be collection of JSON objects.
   *
   * @param mapper Jackson mapper used to parse response
   * @param jsonClass type of ERROR result
   */
  public <E> ResultOrErrorProcessor<T, Collection<E>> orJsonCollectionError(ObjectMapper mapper, Class<E> jsonClass) {
    JsonCollectionConverter<E> converter = new JsonCollectionConverter<>(mapper, jsonClass);
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new ResultOrErrorProcessor<>(this, converter);
  }

  /**
   * Specifies that the type of ERROR result must be map of JSON objects.
   *
   * @param mapper Jackson mapper used to parse response
   * @param jsonKeyClass type of Key of the ERROR (works only with simple types: String, Integer, etc)
   * @param jsonValueClass type of Value of the ERROR
   */
  public <K, V> ResultOrErrorProcessor<T, Map<K, V>> orJsonMapError(ObjectMapper mapper, Class<K> jsonKeyClass, Class<V> jsonValueClass) {
    JsonMapConverter<K, V> converter = new JsonMapConverter<>(mapper, jsonKeyClass, jsonValueClass);
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new ResultOrErrorProcessor<>(this, converter);
  }

  /**
   * Specifies that the type of ERROR result must be PROTOBUF.
   *
   * @param protobufClass type of ERROR result
   */
  public <E extends GeneratedMessageV3> ResultOrErrorProcessor<T, E> orProtobufError(Class<E> protobufClass) {
    ProtobufConverter<E> converter = new ProtobufConverter<>(protobufClass);
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new ResultOrErrorProcessor<>(this, converter);
  }

  /**
   * Specifies that the type of ERROR result must be plain text with {@link PlainTextConverter#DEFAULT default} encoding.
   */
  public ResultOrErrorProcessor<T, String> orPlainTextError() {
    PlainTextConverter converter = new PlainTextConverter();
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new ResultOrErrorProcessor<>(this, converter);
  }

  /**
   * Specifies that the type of ERROR result must be plain text.
   *
   * @param charset used to decode response
   */
  public ResultOrErrorProcessor<T, String> orPlainTextError(Charset charset) {
    PlainTextConverter converter = new PlainTextConverter(charset);
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new ResultOrErrorProcessor<>(this, converter);
  }

  /**
   * Specifies the converter for the ERROR result.
   *
   * @param converter used to convert response to expected ERROR result
   */
  public <E> ResultOrErrorProcessor<T, E> orError(TypeConverter<E> converter) {
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new ResultOrErrorProcessor<>(this, converter);
  }
}
