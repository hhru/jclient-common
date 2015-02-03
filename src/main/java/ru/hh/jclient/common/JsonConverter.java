package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ning.http.client.Response;

public class JsonConverter<T> extends AbstractConverter<T> {

  private ObjectMapper objectMapper;
  private Class<T> jsonClass;

  JsonConverter(ObjectMapper objectMapper, Class<T> jsonClass) {
    this.objectMapper = requireNonNull(objectMapper, "mapper must not be null");
    this.jsonClass = requireNonNull(jsonClass, "jsonClass must not be null");
  }

  @Override
  protected FailableFunction<Response, ResponseWrapper<T>, Exception> converterFunction() {
    return r -> new ResponseWrapper<>(objectMapper.readValue(r.getResponseBodyAsStream(), jsonClass), r);
  }

}
