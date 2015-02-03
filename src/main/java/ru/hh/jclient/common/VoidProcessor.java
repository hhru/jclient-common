package ru.hh.jclient.common;

import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.ning.http.client.Response;

public class VoidProcessor extends AbstractProcessor<Void> {

  public VoidProcessor(HttpClient httpClient) {
    super(httpClient);
  }

  @Override
  protected FailableFunction<Response, ResponseWrapper<Void>, Exception> converterFunction() {
    return r -> new ResponseWrapper<>(null, r);
  }
}
