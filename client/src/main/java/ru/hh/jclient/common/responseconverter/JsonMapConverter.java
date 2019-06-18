package ru.hh.jclient.common.responseconverter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;

import java.util.Collection;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;


public class JsonMapConverter<K, V> extends SingleTypeConverter<Map<K, V>> {

  private static final JavaType[] EMPTY_PARAMETERS = {};
  private final ObjectMapper objectMapper;
  private final JavaType mapType;

  private JsonMapConverter(ObjectMapper objectMapper, JavaType jsonKeyType, JavaType jsonValueType) {
    this.objectMapper = objectMapper;
    mapType = objectMapper.getTypeFactory().constructMapType(
      Map.class,
      requireNonNull(jsonKeyType, "jsonKeyType must not be null"),
      requireNonNull(jsonValueType, "jsonValueType must not be null")
    );
  }

  public JsonMapConverter(ObjectMapper objectMapper, Class<K> jsonKeyClass, TypeReference<V> jsonValueClass) {
    this(requireNonNull(objectMapper, "objectMapper must not be null"),
        ofNullable(jsonKeyClass)
          .map(clazz -> objectMapper.getTypeFactory().constructSimpleType(clazz, EMPTY_PARAMETERS)).orElse(null),
        ofNullable(jsonValueClass)
            .map(typeRef -> objectMapper.getTypeFactory().constructType(typeRef)).orElse(null)
    );
  }

  public JsonMapConverter(ObjectMapper objectMapper, Class<K> jsonKeyClass, Class<V> jsonValueClass) {
    this(requireNonNull(objectMapper, "objectMapper must not be null"),
      ofNullable(jsonKeyClass)
        .map(clazz -> objectMapper.getTypeFactory().constructSimpleType(clazz, EMPTY_PARAMETERS)).orElse(null),
      ofNullable(jsonValueClass)
        .map(clazz -> objectMapper.getTypeFactory().constructSimpleType(clazz, EMPTY_PARAMETERS)).orElse(null)
    );
  }

  @Override
  public FailableFunction<Response, ResultWithResponse<Map<K, V>>, Exception> singleTypeConverterFunction() {
    return r -> new ResultWithResponse<>(objectMapper.readValue(r.getResponseBodyAsStream(), mapType), r);
  }

  @Override
  protected Collection<MediaType> getMediaTypes() {
    return JsonConverter.MEDIA_TYPES;
  }
}
