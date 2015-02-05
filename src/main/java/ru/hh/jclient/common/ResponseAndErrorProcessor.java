package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;
import static ru.hh.jclient.common.HttpClient.OK_RANGE;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import ru.hh.jclient.common.converter.TypeConverter;
import ru.hh.jclient.common.exception.ClientResponseException;
import ru.hh.jclient.common.exception.ResponseConverterException;
import com.google.common.collect.Range;
import com.ning.http.client.Response;

public class ResponseAndErrorProcessor<T, E> {

  private ResponseProcessor<T> responseProcessor;
  private TypeConverter<E> errorConverter;
  private boolean ignoreContentType;
  private Range<Integer> errorsRange = Range.greaterThan(OK_RANGE.upperEndpoint());

  ResponseAndErrorProcessor(ResponseProcessor<T> responseProcessor, TypeConverter<E> errorConverter) {
    this.responseProcessor = requireNonNull(responseProcessor, "responseProcessor must not be null");
    this.errorConverter = requireNonNull(errorConverter, "errorConverter must not be null");
  }

  /**
   * Specifies that "Content-Type" header must be ignored when converting ERROR response.
   */
  public ResponseAndErrorProcessor<T, E> ignoreContentType() {
    this.ignoreContentType = true;
    return this;
  }

  /**
   * Specifies HTTP status code that is eligible for ERROR response parsing. It must not intersect with {@link HttpClient#OK_RANGE}.
   * 
   * @param status HTTP status code that converter will be used for
   */
  public ResponseAndErrorProcessor<T, E> forStatus(int status) {
    return forStatus(Range.singleton(status));
  }

  /**
   * Specifies range of HTTP status codes that are eligible for ERROR response parsing. It must not intersect with {@link HttpClient#OK_RANGE}.
   * 
   * @param status HTTP status codes that converter will be used for
   */
  public ResponseAndErrorProcessor<T, E> forStatus(Range<Integer> statusCodes) {
    if (OK_RANGE.isConnected(statusCodes)) {
      throw new IllegalArgumentException(String.format("Statuses %s are intersect with non-error statuses", statusCodes.toString()));
    }
    this.errorsRange = statusCodes;
    return this;
  }

  /**
   * Returns wrapper consisting of:
   * <ul>
   * <li>expected result, if HTTP status code is in {@link HttpClient#OK_RANGE}, otherwise {@link Optional#empty()}</li>
   * <li>error result, if HTTP status code is NOT in {@link HttpClient#OK_RANGE}, otherwise {@link Optional#empty()}</li>
   * <li>response object</li>
   * </ul>
   * 
   * By default ERROR result will be parsed if HTTP status code is not in {@link HttpClient#OK_RANGE}. More specific range can be specified using
   * {@link #forStatus(Range)} method. Once called, any errors not in that range will NOT be parsed and can be handled manually.
   * 
   * @return {@link ResponseAndErrorWrapper} object with results of response processing
   * @throws ResponseConverterException if failed to process response with either normal or error converter
   */
  public CompletableFuture<ResponseAndErrorWrapper<T, E>> request() {
    return responseProcessor.getHttpClient().request().thenApply(this::wrapResponseAndError);
  }

  private ResponseAndErrorWrapper<T, E> wrapResponseAndError(Response response) {
    Optional<T> value;
    Optional<E> errorValue;
    try {
      if (HttpClient.OK_RESPONSE.apply(response)) {
        value = Optional.of(responseProcessor.getConverter().converterFunction(responseProcessor.isIgnoreContentType()).apply(response).get());
        errorValue = Optional.empty();
      }
      else {
        value = Optional.empty();
        errorValue = parseError(response);
      }
      return new ResponseAndErrorWrapper<T, E>(value, errorValue, response);
    }
    catch (ClientResponseException e) {
      throw e;
    }
    catch (Exception e) {
      ResponseConverterException rce = new ResponseConverterException("Failed to convert response", e);
      responseProcessor.getHttpClient().getDebug().onConverterProblem(rce);
      throw rce;
    }
    finally {
      responseProcessor.getHttpClient().getDebug().onProcessingFinished();
    }
  }

  private Optional<E> parseError(Response response) throws Exception {
    if (errorsRange.contains(response.getStatusCode())) {
      return Optional.of(errorConverter.converterFunction(ignoreContentType).apply(response).get());
    }
    return Optional.empty();
  }

}
