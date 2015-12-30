package ru.hh.jclient.common.converter;

import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;

import java.util.Collection;
import java.util.Optional;

import com.google.common.net.MediaType;
import com.ning.http.client.Response;

/**
 * Provides {@link FailableFunction} that converts {@link Response} to {@link ResultWithResponse} with result of provided type.
 *
 * @param <T> type of conversion result
 */
public interface TypeConverter<T> {

  FailableFunction<Response, ResultWithResponse<T>, Exception> converterFunction();

  Optional<Collection<MediaType>> getSupportedMediaTypes();

}
