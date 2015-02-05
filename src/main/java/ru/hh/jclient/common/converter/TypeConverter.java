package ru.hh.jclient.common.converter;

import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.ning.http.client.Response;

/**
 * Provides {@link FailableFunction} that converts {@link Response} to {@link ResponseWrapper} with result of provided type.
 *
 * @param <T> type of conversion result
 */
public interface TypeConverter<T> {

  FailableFunction<Response, ResponseWrapper<T>, Exception> converterFunction(boolean ignoreContentType);

}
