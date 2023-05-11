package ru.hh.jclient.common.responseconverter;

import java.util.Collection;
import static java.util.Optional.ofNullable;
import java.util.Set;
import static ru.hh.jclient.common.HttpHeaderNames.CONTENT_TYPE;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.exception.NoContentTypeException;
import ru.hh.jclient.common.exception.UnexpectedContentTypeException;
import ru.hh.jclient.common.util.ContentType;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;

/**
 * Converter that knows how to convert response to exactly one type of result. Knows and checks expected content type.
 *
 * @param <T> type of result
 */
public abstract class SingleTypeConverter<T> implements TypeConverter<T> {

  /**
   * Returns list of allowed content types. Response' "Content-Type" header will be checked against this list.
   *
   * @return list of allowed media types
   */
  protected Collection<String> getContentTypes() {
    return Set.of();
  }

  /**
   * Returns converter function that, ignoring content type, just converts the response.
   *
   * @return converter function
   */
  public abstract FailableFunction<Response, ResultWithResponse<T>, Exception> singleTypeConverterFunction();

  @Override
  public Collection<String> getSupportedContentTypes() {
    return getContentTypes();
  }

  @Override
  public FailableFunction<Response, ResultWithResponse<T>, Exception> converterFunction() {
    FailableFunction<Response, Response, Exception> checkFunction = this::checkContentType;
    return checkFunction.andThen(singleTypeConverterFunction());
  }

  private Response checkContentType(Response r) {
    String contentTypeString = r.getHeader(CONTENT_TYPE);
    ContentType contentType = ofNullable(contentTypeString).map(ContentType::new).orElseThrow(() -> new NoContentTypeException(r));

    if (getContentTypes().stream().map(ContentType::new).noneMatch(ct -> ct.allows(contentType))) {
      throw new UnexpectedContentTypeException(r, contentTypeString, getContentTypes());
    }
    return r;
  }

}
