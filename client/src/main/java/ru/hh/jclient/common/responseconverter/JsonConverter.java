package ru.hh.jclient.common.responseconverter;

import static java.util.Objects.requireNonNull;
import static java.util.Set.of;
import static ru.hh.jclient.common.util.ContentType.APPLICATION_JSON;
import java.util.Collection;
import java.util.Set;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonConverter<T> extends SingleTypeConverter<T> {

  static final Set<String> MEDIA_TYPES = of(APPLICATION_JSON);

  private ObjectMapper objectMapper;
  private Class<T> jsonClass;

  public JsonConverter(ObjectMapper objectMapper, Class<T> jsonClass) {
    this.objectMapper = requireNonNull(objectMapper, "objectMapper must not be null");
    this.jsonClass = requireNonNull(jsonClass, "jsonClass must not be null");
  }

  @Override
  public FailableFunction<Response, ResultWithResponse<T>, Exception> singleTypeConverterFunction() {
    return r -> new ResultWithResponse<>(objectMapper.readValue(r.getResponseBodyAsStream(), jsonClass), r);
  }

  @Override
  protected Collection<String> getContentTypes() {
    return MEDIA_TYPES;
  }

}
