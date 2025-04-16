package ru.hh.jclient.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.GeneratedMessageV3;
import jakarta.xml.bind.JAXBContext;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import ru.hh.jclient.common.responseconverter.JsonCollectionConverter;
import ru.hh.jclient.common.responseconverter.JsonConverter;
import ru.hh.jclient.common.responseconverter.JsonMapConverter;
import ru.hh.jclient.common.responseconverter.PlainTextConverter;
import ru.hh.jclient.common.responseconverter.ProtobufConverter;
import ru.hh.jclient.common.responseconverter.TypeConverter;
import ru.hh.jclient.common.responseconverter.XmlConverter;

public class EmptyResultProcessor extends ResultProcessor<Void> {

  EmptyResultProcessor(HttpClient httpClient, TypeConverter<Void> converter) {
    super(httpClient, converter);
  }

  public CompletableFuture<EmptyWithStatus> emptyWithStatus() {
    return super.resultWithStatus().thenApply(rws -> new EmptyWithStatus(rws.getStatusCode()));
  }

  /**
   * @deprecated Use {@link #emptyWithStatus()}
   */
  @Override
  @Deprecated // use #emptyWithStatus()
  public CompletableFuture<ResultWithStatus<Void>> resultWithStatus() {
    return super.resultWithStatus();
  }

  @Override
  public <E> EmptyOrErrorProcessor<E> orXmlError(JAXBContext context, Class<E> xmlClass) {
    XmlConverter<E> converter = new XmlConverter<>(context, xmlClass);
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new EmptyOrErrorProcessor<>(this, converter);
  }

  @Override
  public <E> EmptyOrErrorProcessor<E> orJsonError(ObjectMapper mapper, Class<E> jsonClass) {
    JsonConverter<E> converter = new JsonConverter<>(mapper, jsonClass);
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new EmptyOrErrorProcessor<>(this, converter);
  }

  @Override
  public <E> EmptyOrErrorProcessor<Collection<E>> orJsonCollectionError(ObjectMapper mapper, Class<E> jsonClass) {
    JsonCollectionConverter<E> converter = new JsonCollectionConverter<>(mapper, jsonClass);
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new EmptyOrErrorProcessor<>(this, converter);
  }

  @Override
  public <K, V> EmptyOrErrorProcessor<Map<K, V>> orJsonMapError(ObjectMapper mapper, Class<K> jsonKeyClass, Class<V> jsonValueClass) {
    JsonMapConverter<K, V> converter = new JsonMapConverter<>(mapper, jsonKeyClass, jsonValueClass);
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new EmptyOrErrorProcessor<>(this, converter);
  }

  @Override
  public <E extends GeneratedMessageV3> EmptyOrErrorProcessor<E> orProtobufError(Class<E> protobufClass) {
    ProtobufConverter<E> converter = new ProtobufConverter<>(protobufClass);
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new EmptyOrErrorProcessor<>(this, converter);
  }

  @Override
  public EmptyOrErrorProcessor<String> orPlainTextError() {
    PlainTextConverter converter = new PlainTextConverter();
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new EmptyOrErrorProcessor<>(this, converter);
  }

  @Override
  public EmptyOrErrorProcessor<String> orPlainTextError(Charset charset) {
    PlainTextConverter converter = new PlainTextConverter(charset);
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new EmptyOrErrorProcessor<>(this, converter);
  }

  @Override
  public <E> EmptyOrErrorProcessor<E> orError(TypeConverter<E> converter) {
    getHttpClient().setExpectedMediaTypesForErrors(converter.getSupportedContentTypes());
    return new EmptyOrErrorProcessor<>(this, converter);
  }
}
