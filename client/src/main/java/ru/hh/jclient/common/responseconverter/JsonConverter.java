package ru.hh.jclient.common.responseconverter;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.util.Objects.requireNonNull;
import java.util.Collection;
import java.util.Set;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;

public class JsonConverter<T> extends SingleTypeConverter<T> {

  static final Set<MediaType> MEDIA_TYPES = of(JSON_UTF_8.withoutParameters());

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
  protected Collection<MediaType> getMediaTypes() {
    return MEDIA_TYPES;
  }

}
