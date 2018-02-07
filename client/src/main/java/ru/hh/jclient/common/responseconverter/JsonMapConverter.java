package ru.hh.jclient.common.responseconverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.net.MediaType;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;

import java.util.Collection;
import java.util.Map;

import static java.util.Objects.requireNonNull;


public class JsonMapConverter<K, V> extends SingleTypeConverter<Map<K, V>> {

  private ObjectMapper objectMapper;
  private Class<K> jsonKeyClass;
  private Class<V> jsonValueClass;

  public JsonMapConverter(ObjectMapper objectMapper, Class<K> jsonKeyClass, Class<V> jsonValueClass) {
    this.objectMapper = requireNonNull(objectMapper, "objectMapper must not be null");
    this.jsonKeyClass = requireNonNull(jsonKeyClass, "jsonKeyClass must not be null");
    this.jsonValueClass = requireNonNull(jsonValueClass, "jsonValueClass must not be null");
  }

  @Override
  public FailableFunction<Response, ResultWithResponse<Map<K, V>>, Exception> singleTypeConverterFunction() {
    return r -> new ResultWithResponse<>(
        objectMapper.readValue(r.getResponseBodyAsStream(), TypeFactory.defaultInstance().constructMapType(Map.class,
          jsonKeyClass, jsonValueClass)),
        r);
  }

  @Override
  protected Collection<MediaType> getMediaTypes() {
    return JsonConverter.MEDIA_TYPES;
  }
}
