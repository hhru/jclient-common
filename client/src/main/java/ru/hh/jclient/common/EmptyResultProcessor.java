package ru.hh.jclient.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.GeneratedMessageV3;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.xml.bind.JAXBContext;
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
    return new EmptyOrErrorProcessor<>(this, new XmlConverter<>(context, xmlClass));
  }

  @Override
  public <E> EmptyOrErrorProcessor<E> orJsonError(ObjectMapper mapper, Class<E> jsonClass) {
    return new EmptyOrErrorProcessor<>(this, new JsonConverter<>(mapper, jsonClass));
  }

  @Override
  public <E> EmptyOrErrorProcessor<Collection<E>> orJsonCollectionError(ObjectMapper mapper, Class<E> jsonClass) {
    return new EmptyOrErrorProcessor<>(this, new JsonCollectionConverter<>(mapper, jsonClass));
  }

  @Override
  public <K, V> EmptyOrErrorProcessor<Map<K, V>> orJsonMapError(ObjectMapper mapper, Class<K> jsonKeyClass, Class<V> jsonValueClass) {
    return new EmptyOrErrorProcessor<>(this, new JsonMapConverter<>(mapper, jsonKeyClass, jsonValueClass));
  }

  @Override
  public <E extends GeneratedMessageV3> EmptyOrErrorProcessor<E> orProtobufError(Class<E> protobufClass) {
    return new EmptyOrErrorProcessor<>(this, new ProtobufConverter<>(protobufClass));
  }

  @Override
  public EmptyOrErrorProcessor<String> orPlainTextError() {
    return new EmptyOrErrorProcessor<>(this, new PlainTextConverter());
  }

  @Override
  public EmptyOrErrorProcessor<String> orPlainTextError(Charset charset) {
    return new EmptyOrErrorProcessor<>(this, new PlainTextConverter(charset));
  }

  @Override
  public <E> EmptyOrErrorProcessor<E> orError(TypeConverter<E> converter) {
    return new EmptyOrErrorProcessor<>(this, converter);
  }
}
