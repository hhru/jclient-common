package ru.hh.jclient.common.responseconverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import static java.util.Set.of;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import static ru.hh.jclient.common.util.ContentType.APPLICATION_JSON;
import static ru.hh.jclient.common.util.JsonTypeConverter.convertClassToJavaType;
import static ru.hh.jclient.common.util.JsonTypeConverter.convertReferenceToJavaType;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;

public class JsonConverter<T> extends SingleTypeConverter<T> {

  static final Set<String> MEDIA_TYPES = of(APPLICATION_JSON);

  private ObjectMapper objectMapper;
  private JavaType jsonType;

  private JsonConverter(ObjectMapper objectMapper, JavaType jsonType) {
    this.objectMapper = requireNonNull(objectMapper, "objectMapper must not be null");
    this.jsonType = requireNonNull(jsonType, "jsonClass must not be null");
  }

  public JsonConverter(ObjectMapper objectMapper, Class<T> jsonClass) {
    this(objectMapper, convertClassToJavaType(objectMapper, jsonClass));
  }

  public JsonConverter(ObjectMapper objectMapper, TypeReference<T> jsonClass) {
    this(objectMapper, convertReferenceToJavaType(objectMapper, jsonClass));
  }

  @Override
  public FailableFunction<Response, ResultWithResponse<T>, Exception> singleTypeConverterFunction() {
    return r -> new ResultWithResponse<>(objectMapper.readValue(r.getResponseBodyAsStream(), jsonType), r);
  }

  @Override
  protected Collection<String> getContentTypes() {
    return MEDIA_TYPES;
  }

  @Override
  public FailableFunction<T, String, Exception> reverseConverterFunction() {
    return value -> {
      try {
        return objectMapper.writerFor(jsonType).writeValueAsString(value);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    };
  }
}
