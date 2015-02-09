package ru.hh.jclient.common.converter;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Objects.requireNonNull;
import java.util.Collection;
import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import com.ning.http.client.Response;

public class JsonConverter<T> extends SingleTypeConverter<T> {

  private ObjectMapper objectMapper;
  private Class<T> jsonClass;

  public JsonConverter(ObjectMapper objectMapper, Class<T> jsonClass) {
    this.objectMapper = requireNonNull(objectMapper, "objectMapper must not be null");
    this.jsonClass = requireNonNull(jsonClass, "jsonClass must not be null");
  }

  @Override
  public FailableFunction<Response, ResponseWrapper<T>, Exception> singleTypeConverterFunction() {
    return r -> new ResponseWrapper<>(objectMapper.readValue(r.getResponseBodyAsStream(), jsonClass), r);
  }

  @Override
  public Collection<MediaType> getMediaTypes() {
    return of(MediaType.JSON_UTF_8.withoutParameters());
  }

}
