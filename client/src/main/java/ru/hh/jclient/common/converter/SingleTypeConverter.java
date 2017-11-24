package ru.hh.jclient.common.converter;

import java.util.Collection;
import java.util.Optional;
import ru.hh.jclient.common.ResponseConverterUtils;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.exception.NoContentTypeException;
import ru.hh.jclient.common.exception.UnexpectedContentTypeException;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.ning.http.client.Response;

/**
 * Converter that knows how to convert response to exactly one type of result. Knows and checks expected content type.
 *
 * @param <T>
 *          type of result
 *
 * @deprecated use same class from 'ru.hh.jclient.common.responseconverter'
 */
@Deprecated
public abstract class SingleTypeConverter<T> implements TypeConverter<T> {

  /**
   * Returns list of allowed media types. Response' "Content-Type" header will be checked against this list.
   *
   * @return list of allowed media types
   */
  protected abstract Collection<MediaType> getMediaTypes();

  /**
   * Returns converter function that, ignoring content type, just converts the response.
   *
   * @return converter function
   */
  public abstract FailableFunction<Response, ResultWithResponse<T>, Exception> singleTypeConverterFunction();

  @Override
  public Optional<Collection<MediaType>> getSupportedMediaTypes() {
    return Optional.of(getMediaTypes());
  }

  @Override
  public FailableFunction<Response, ResultWithResponse<T>, Exception> converterFunction() {
    FailableFunction<Response, Response, Exception> checkFunction = this::checkContentType;
    return checkFunction.andThen(singleTypeConverterFunction());
  }

  private Response checkContentType(Response r) throws Exception {
    String contentType = r.getHeader(HttpHeaders.CONTENT_TYPE);
    if (contentType == null) {
      throw new NoContentTypeException(ResponseConverterUtils.convert(r));
    }
    MediaType mt = MediaType.parse(contentType);
    if (getMediaTypes().stream().noneMatch(m -> mt.is(m))) {
      throw new UnexpectedContentTypeException(ResponseConverterUtils.convert(r), mt, getMediaTypes());
    }
    return r;
  }

}
