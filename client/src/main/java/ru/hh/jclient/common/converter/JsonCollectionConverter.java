package ru.hh.jclient.common.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.net.MediaType;
import com.ning.http.client.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import java.util.Collection;
import static java.util.Objects.requireNonNull;


public class JsonCollectionConverter<T> extends SingleTypeConverter<Collection<T>> {

  private ObjectMapper objectMapper;
  private Class<T> jsonClass;

  public JsonCollectionConverter(ObjectMapper objectMapper, Class<T> jsonClass) {
    this.objectMapper = requireNonNull(objectMapper, "objectMapper must not be null");
    this.jsonClass = requireNonNull(jsonClass, "jsonClass must not be null");
  }


  @Override
  public FailableFunction<Response, ResultWithResponse<Collection<T>>, Exception> singleTypeConverterFunction() {
    return r -> new ResultWithResponse<>(
        objectMapper.readValue(r.getResponseBodyAsStream(), TypeFactory.defaultInstance().constructCollectionType(Collection.class, jsonClass)),
        r);
  }

  @Override
  protected Collection<MediaType> getMediaTypes() {
    return JsonConverter.MEDIA_TYPES;
  }
}
