package ru.hh.jclient.common.responseconverter;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;

/**
 * Provides {@link FailableFunction} that converts {@link Response} to {@link ResultWithResponse} with result of provided type.
 *
 * @param <T> type of conversion result
 */
public interface TypeConverter<T> {

  FailableFunction<Response, ResultWithResponse<T>, Exception> converterFunction();

  default Function<T, String> reverseConverterFunction() {
    throw new UnsupportedOperationException("Method was not implemented");
  }

  default Optional<Collection<String>> getSupportedContentTypes() {
    return Optional.empty();
  }
}
