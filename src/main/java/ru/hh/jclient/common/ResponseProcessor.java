package ru.hh.jclient.common;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import javax.xml.bind.JAXBContext;

import ru.hh.jclient.common.exception.ClientResponseException;
import ru.hh.jclient.common.exception.ResponseConverterException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.GeneratedMessage;
import com.ning.http.client.Response;

public class ResponseProcessor<T> {

  private HttpClient httpClient;
  private AbstractConverter<T> converter;

  ResponseProcessor(HttpClient httpClient, AbstractConverter<T> converter) {
    this.httpClient = httpClient;
    this.converter = converter;
  }

  HttpClient getHttpClient() {
    return httpClient;
  }

  AbstractConverter<T> getConverter() {
    return converter;
  }

  public CompletableFuture<T> request() {
    return wrappedRequest().thenApply(ResponseWrapper::get);
  }

  public CompletableFuture<ResponseWrapper<T>> wrappedRequest() {
    return uncheckedRequest().thenApply(this::statusChecker).thenApply(this::responseWrapper);
  }

  CompletableFuture<Response> uncheckedRequest() {
    return httpClient.executeRequest();
  }

  private Response statusChecker(Response response) {
    try {
      if (HttpClient.OK_RESPONSE.apply(response)) {
        return response;
      }
      throw new ClientResponseException(response);
    }
    finally {
      httpClient.getDebug().onProcessingFinished();
    }
  }

  private ResponseWrapper<T> responseWrapper(Response response) {
    try {
      return converter.converterFunction().apply(response);
    }
    catch (Exception e) {
      ResponseConverterException rce = new ResponseConverterException("Failed to convert response", e);
      httpClient.getDebug().onConverterProblem(rce);
      throw rce;
    }
    finally {
      httpClient.getDebug().onProcessingFinished();
    }
  }

  public <E> ResponseAndErrorProcessor<T, E> orXmlError(JAXBContext context, Class<E> xmlClass) {
    return new ResponseAndErrorProcessor<T, E>(this, new XmlConverter<>(context, xmlClass));
  }

  public <E> ResponseAndErrorProcessor<T, E> orJsonError(ObjectMapper mapper, Class<E> jsonClass) {
    return new ResponseAndErrorProcessor<T, E>(this, new JsonConverter<>(mapper, jsonClass));
  }

  public <E extends GeneratedMessage> ResponseAndErrorProcessor<T, E> orProtobufError(Class<E> protobufClass) {
    return new ResponseAndErrorProcessor<T, E>(this, new ProtobufConverter<>(protobufClass));
  }

  public ResponseAndErrorProcessor<T, String> orPlainTextError() {
    return new ResponseAndErrorProcessor<T, String>(this, new PlainTextConverter());
  }

  public ResponseAndErrorProcessor<T, String> orPlainTextError(Charset charset) {
    return new ResponseAndErrorProcessor<T, String>(this, new PlainTextConverter(charset));
  }

}
