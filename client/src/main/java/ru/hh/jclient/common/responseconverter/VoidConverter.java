package ru.hh.jclient.common.responseconverter;

import ru.hh.jclient.common.EmptyWithResponse;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;

import java.util.Collection;
import java.util.Optional;

public class VoidConverter implements TypeConverter<Void> {

  @Override
  public FailableFunction<Response, ResultWithResponse<Void>, Exception> converterFunction() {
    return EmptyWithResponse::new;
  }

  @Override
  public Optional<Collection<String>> getSupportedMediaTypes() {
    return Optional.empty();
  }
}
