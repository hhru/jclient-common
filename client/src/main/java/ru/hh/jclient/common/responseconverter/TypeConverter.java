package ru.hh.jclient.common.responseconverter;

import com.google.common.net.MediaType;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;

import java.util.Collection;
import java.util.Optional;
import static java.util.stream.Collectors.toList;

/**
 * Provides {@link FailableFunction} that converts {@link Response} to {@link ResultWithResponse} with result of provided type.
 *
 * @param <T> type of conversion result
 */
public interface TypeConverter<T> {

  FailableFunction<Response, ResultWithResponse<T>, Exception> converterFunction();

  default Optional<Collection<String>> getSupportedContentTypes() {
    return getSupportedMediaTypes().map(mt -> mt.stream().map(MediaType::toString).collect(toList()));
  }

  // override getSupportedContentTypes()
  @Deprecated(forRemoval = true)
  default Optional<Collection<MediaType>> getSupportedMediaTypes() {
    return Optional.empty();
  }

}
