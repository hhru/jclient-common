package ru.hh.jclient.common.responseconverter;

import java.util.Collection;
import java.util.Optional;
import ru.hh.jclient.common.EmptyWithResponse;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;

public class VoidConverter implements TypeConverter<Void> {

  @Override
  public FailableFunction<Response, ResultWithResponse<Void>, Exception> converterFunction() {
    return EmptyWithResponse::new;
  }

  @Override
  public Optional<Collection<String>> getSupportedContentTypes() {
    return Optional.empty();
  }
}
