package ru.hh.jclient.common.converter;

import java.util.Collection;
import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.jclient.common.exception.NoContentTypeException;
import ru.hh.jclient.common.exception.UnexpectedContentTypeException;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.ning.http.client.Response;

public abstract class SingleTypeConverter<T> implements TypeConverter<T> {

  public abstract Collection<MediaType> getMediaTypes();

  public abstract FailableFunction<Response, ResponseWrapper<T>, Exception> converterFunction();

  @Override
  public FailableFunction<Response, ResponseWrapper<T>, Exception> converterFunction(boolean ignoreContentType) {
    if (ignoreContentType) {
      return converterFunction();
    }
    FailableFunction<Response, Response, Exception> checkFunction = r -> {
      String contentType = r.getHeader(HttpHeaders.CONTENT_TYPE);
      if (contentType == null) {
        throw new NoContentTypeException(r);
      }
      MediaType mt = MediaType.parse(contentType);
      if (getMediaTypes().stream().noneMatch(m -> mt.is(m))) {
        throw new UnexpectedContentTypeException(r, mt, getMediaTypes());
      }
      return r;
    };
    return checkFunction.andThen(converterFunction());
  }

}
