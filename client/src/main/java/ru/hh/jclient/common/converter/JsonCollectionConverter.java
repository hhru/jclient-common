package ru.hh.jclient.common.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.ning.http.client.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces;

import java.util.Collection;

import static java.util.Objects.requireNonNull;


public class JsonCollectionConverter<T> extends JsonConverter<Collection<T>> {

  protected Class<T> jsonClass;

  public JsonCollectionConverter(ObjectMapper objectMapper, Class<T> jsonClass) {
    super(objectMapper);
    this.jsonClass = requireNonNull(jsonClass, "jsonClass must not be null");
  }


  @Override
  public MoreFunctionalInterfaces.FailableFunction<Response, ResultWithResponse<Collection<T>>, Exception> singleTypeConverterFunction() {
    return r -> new ResultWithResponse<>(
        objectMapper.readValue(r.getResponseBodyAsStream(), TypeFactory.defaultInstance()
            .constructCollectionType(Collection.class, jsonClass)), r);
  }
}
