package ru.hh.jclient.common.converter;

import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;

import java.util.Collection;
import java.util.Optional;

import com.google.common.net.MediaType;
import com.ning.http.client.Response;

public class VoidConverter implements TypeConverter<Void> {

  @Override
  public FailableFunction<Response, ResultWithResponse<Void>, Exception> converterFunction() {
    return r -> new ResultWithResponse<>(null, r);
  }

  @Override
  public Optional<Collection<MediaType>> getSupportedMediaTypes() {
    return Optional.empty();
  }
}
