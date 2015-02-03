package ru.hh.jclient.common;

import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.ning.http.client.Response;

public class VoidConverter extends AbstractConverter<Void> {

  @Override
  protected FailableFunction<Response, ResponseWrapper<Void>, Exception> converterFunction() {
    return r -> new ResponseWrapper<>(null, r);
  }
}
