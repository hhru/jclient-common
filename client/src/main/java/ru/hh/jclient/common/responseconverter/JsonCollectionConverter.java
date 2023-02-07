package ru.hh.jclient.common.responseconverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import static java.util.Objects.requireNonNull;
import java.util.function.Function;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.JsonTypeConverter;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;


public class JsonCollectionConverter<T> extends SingleTypeConverter<Collection<T>> {

  private final ObjectMapper objectMapper;
  private final JavaType elementType;

  private JsonCollectionConverter(ObjectMapper objectMapper, JavaType elementType) {
    this.objectMapper = requireNonNull(objectMapper, "objectMapper must not be null");
    this.elementType = objectMapper.getTypeFactory()
        .constructCollectionType(Collection.class, requireNonNull(elementType, "jsonClass must not be null"));
  }

  public JsonCollectionConverter(ObjectMapper objectMapper, Class<T> jsonClass) {
    this(objectMapper, JsonTypeConverter.convertClassToJavaType(objectMapper, jsonClass));
  }

  public JsonCollectionConverter(ObjectMapper objectMapper, TypeReference<T> jsonClass) {
    this(objectMapper, JsonTypeConverter.convertReferenceToJavaType(objectMapper, jsonClass));
  }


  @Override
  public FailableFunction<Response, ResultWithResponse<Collection<T>>, Exception> singleTypeConverterFunction() {
    return r -> new ResultWithResponse<>(objectMapper.readValue(r.getResponseBodyAsStream(), elementType), r);
  }

  @Override
  protected Collection<String> getContentTypes() {
    return JsonConverter.MEDIA_TYPES;
  }

  @Override
  public Function<Collection<T>, String> reverseConverterFunction() {
    return value -> {
      try {
        return objectMapper.writerFor(elementType).writeValueAsString(value);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    };
  }
}
