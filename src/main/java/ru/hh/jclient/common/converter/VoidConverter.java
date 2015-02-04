package ru.hh.jclient.common.converter;

import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.ning.http.client.Response;

public class VoidConverter implements TypeConverter<Void> {

  @Override
  public FailableFunction<Response, ResponseWrapper<Void>, Exception> converterFunction() {
    return r -> new ResponseWrapper<>(null, r);
  }
}
