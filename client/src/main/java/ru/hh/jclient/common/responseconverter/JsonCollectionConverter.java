package ru.hh.jclient.common.responseconverter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import java.util.Collection;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;


public class JsonCollectionConverter<T> extends SingleTypeConverter<Collection<T>> {

  private static final JavaType[] EMPTY_PARAMETERS = {};
  private final ObjectMapper objectMapper;
  private final JavaType elementType;

  private JsonCollectionConverter(ObjectMapper objectMapper, JavaType elementType) {
    this.objectMapper = objectMapper;
    this.elementType = objectMapper.getTypeFactory()
        .constructCollectionType(Collection.class, requireNonNull(elementType, "jsonClass must not be null"));
  }

  public JsonCollectionConverter(ObjectMapper objectMapper, Class<T> jsonClass) {
    this(requireNonNull(objectMapper, "objectMapper must not be null"),
        ofNullable(jsonClass).map(clazz -> objectMapper.getTypeFactory().constructSimpleType(clazz, EMPTY_PARAMETERS)).orElse(null)
    );
  }

  public JsonCollectionConverter(ObjectMapper objectMapper, TypeReference<T> jsonClass) {
    this(requireNonNull(objectMapper, "objectMapper must not be null"),
        ofNullable(jsonClass).map(clazz -> objectMapper.getTypeFactory().constructType(clazz)).orElse(null)
    );
  }


  @Override
  public FailableFunction<Response, ResultWithResponse<Collection<T>>, Exception> singleTypeConverterFunction() {
    return r -> new ResultWithResponse<>(objectMapper.readValue(r.getResponseBodyAsStream(), elementType), r);
  }

  @Override
  protected Collection<MediaType> getMediaTypes() {
    return JsonConverter.MEDIA_TYPES;
  }
}
