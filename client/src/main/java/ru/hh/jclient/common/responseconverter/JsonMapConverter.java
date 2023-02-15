package ru.hh.jclient.common.responseconverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import static ru.hh.jclient.common.util.JsonTypeConverter.convertClassToJavaType;
import static ru.hh.jclient.common.util.JsonTypeConverter.convertReferenceToJavaType;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;


public class JsonMapConverter<K, V> extends SingleTypeConverter<Map<K, V>> {

  private final ObjectMapper objectMapper;
  private final JavaType mapType;

  private JsonMapConverter(ObjectMapper objectMapper, JavaType jsonKeyType, JavaType jsonValueType) {
    this.objectMapper = requireNonNull(objectMapper, "objectMapper must not be null");
    mapType = objectMapper.getTypeFactory().constructMapType(
      Map.class,
      requireNonNull(jsonKeyType, "jsonKeyType must not be null"),
      requireNonNull(jsonValueType, "jsonValueType must not be null")
    );
  }

  public JsonMapConverter(ObjectMapper objectMapper, Class<K> jsonKeyClass, TypeReference<V> jsonValueClass) {
    this(objectMapper,
        convertClassToJavaType(objectMapper, jsonKeyClass),
        convertReferenceToJavaType(objectMapper, jsonValueClass)
    );
  }

  public JsonMapConverter(ObjectMapper objectMapper, Class<K> jsonKeyClass, Class<V> jsonValueClass) {
    this(objectMapper,
        convertClassToJavaType(objectMapper, jsonKeyClass),
        convertClassToJavaType(objectMapper, jsonValueClass)
    );
  }

  @Override
  public FailableFunction<Response, ResultWithResponse<Map<K, V>>, Exception> singleTypeConverterFunction() {
    return r -> new ResultWithResponse<>(objectMapper.readValue(r.getResponseBodyAsStream(), mapType), r);
  }

  @Override
  protected Collection<String> getContentTypes() {
    return JsonConverter.MEDIA_TYPES;
  }

  @Override
  public FailableFunction<Map<K, V>, String, Exception> reverseConverterFunction() {
    return value -> {
      try {
        return objectMapper.writerFor(mapType).writeValueAsString(value);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    };
  }
}
