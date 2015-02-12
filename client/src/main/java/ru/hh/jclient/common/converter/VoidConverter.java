package ru.hh.jclient.common.converter;

import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.ning.http.client.Response;

public class VoidConverter implements TypeConverter<Void> {

  @Override
  public FailableFunction<Response, ResultWithResponse<Void>, Exception> converterFunction() {
    return r -> new ResultWithResponse<>(null, r);
  }
}
