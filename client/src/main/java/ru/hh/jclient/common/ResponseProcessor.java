package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import javax.xml.bind.JAXBContext;
import ru.hh.jclient.common.converter.TypeConverter;
import ru.hh.jclient.common.converter.JsonConverter;
import ru.hh.jclient.common.converter.PlainTextConverter;
import ru.hh.jclient.common.converter.ProtobufConverter;
import ru.hh.jclient.common.converter.XmlConverter;
import ru.hh.jclient.common.exception.ClientResponseException;
import ru.hh.jclient.common.exception.ResponseConverterException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.GeneratedMessage;
import com.ning.http.client.Response;

public class ResponseProcessor<T> {

  private HttpClient httpClient;
  private TypeConverter<T> converter;

  ResponseProcessor(HttpClient httpClient, TypeConverter<T> converter) {
    this.httpClient = requireNonNull(httpClient, "http client must not be null");
    this.converter = requireNonNull(converter, "converter must not be null");
  }

  HttpClient getHttpClient() {
    return httpClient;
  }

  TypeConverter<T> getConverter() {
    return converter;
  }

  /**
   * Returns expected result or throws {@link ClientResponseException}.
   * 
   * @return expected result
   * @throws ClientResponseException if status code is not in {@link HttpClient#OK_RANGE}
   * @throws ResponseConverterException if converter failed to process response
   */
  public CompletableFuture<T> request() {
    return httpClient.request().thenApply(this::wrapOrThrow).thenApply(rw -> rw.get().orElse(null));
  }

  /**
   * Returns wrapper with response object and result (empty if status code is not in {@link HttpClient#OK_RANGE}).
   * 
   * @return {@link ResponseWrapper} with expected result (possibly empty) and response object
   * @throws ResponseConverterException if converter failed to process response
   */
  public CompletableFuture<ResponseWrapper<T>> wrappedRequest() {
    return httpClient.request().thenApply(this::wrapOrNull);
  }

  private ResponseWrapper<T> wrapOrThrow(Response response) {
    try {
      if (HttpClient.OK_RESPONSE.apply(response)) {
        return wrap(response);
      }
      throw new ClientResponseException(response);
    }
    finally {
      httpClient.getDebug().onProcessingFinished();
    }
  }

  private ResponseWrapper<T> wrapOrNull(Response response) {
    try {
      if (HttpClient.OK_RESPONSE.apply(response)) {
        return wrap(response);
      }
      return new ResponseWrapper<T>(null, response);
    }
    finally {
      httpClient.getDebug().onProcessingFinished();
    }
  }

  private ResponseWrapper<T> wrap(Response response) {
    try {
      return converter.converterFunction().apply(response);
    }
    catch (ClientResponseException e) {
      throw e;
    }
    catch (Exception e) {
      ResponseConverterException rce = new ResponseConverterException("Failed to convert response", e);
      httpClient.getDebug().onConverterProblem(rce);
      throw rce;
    }
  }

  /**
   * Specifies that the type of ERROR result must be XML.
   * 
   * @param context JAXB context used to parse response
   * @param xmlClass type of ERROR result
   */
  public <E> ResponseAndErrorProcessor<T, E> orXmlError(JAXBContext context, Class<E> xmlClass) {
    return new ResponseAndErrorProcessor<T, E>(this, new XmlConverter<>(context, xmlClass));
  }

  /**
   * Specifies that the type of ERROR result must be JSON.
   * 
   * @param mapper Jackson mapper used to parse response
   * @param jsonClass type of ERROR result
   */
  public <E> ResponseAndErrorProcessor<T, E> orJsonError(ObjectMapper mapper, Class<E> jsonClass) {
    return new ResponseAndErrorProcessor<T, E>(this, new JsonConverter<>(mapper, jsonClass));
  }

  /**
   * Specifies that the type of ERROR result must be PROTOBUF.
   * 
   * @param protobufClass type of ERROR result
   */
  public <E extends GeneratedMessage> ResponseAndErrorProcessor<T, E> orProtobufError(Class<E> protobufClass) {
    return new ResponseAndErrorProcessor<T, E>(this, new ProtobufConverter<>(protobufClass));
  }

  /**
   * Specifies that the type of ERROR result must be plain text with {@link PlainTextConverter#DEFAULT default} encoding.
   */
  public ResponseAndErrorProcessor<T, String> orPlainTextError() {
    return new ResponseAndErrorProcessor<T, String>(this, new PlainTextConverter());
  }

  /**
   * Specifies that the type of ERROR result must be plain text.
   * 
   * @param charset used to decode response
   */
  public ResponseAndErrorProcessor<T, String> orPlainTextError(Charset charset) {
    return new ResponseAndErrorProcessor<T, String>(this, new PlainTextConverter(charset));
  }

  /**
   * Specifies the converter for the ERROR result.
   * 
   * @param converter used to convert response to expected ERROR result
   */
  public <E> ResponseAndErrorProcessor<T, E> orError(TypeConverter<E> converter) {
    return new ResponseAndErrorProcessor<T, E>(this, converter);
  }
}
