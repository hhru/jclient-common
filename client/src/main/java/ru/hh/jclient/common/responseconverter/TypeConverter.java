package ru.hh.jclient.common.responseconverter;

import java.util.Collection;
import java.util.List;
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

  /**
   * @return function that could be used for data serialization
   */
  default FailableFunction<T, String, Exception> reverseConverterFunction() {
    throw new UnsupportedOperationException("Method was not implemented");
  }

  default Collection<String> getSupportedContentTypes() {
    return List.of();
  }
}
