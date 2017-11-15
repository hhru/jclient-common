package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;
import static ru.hh.jclient.common.HttpClient.OK_RANGE;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import ru.hh.jclient.common.responseconverter.TypeConverter;
import ru.hh.jclient.common.exception.ClientResponseException;
import ru.hh.jclient.common.exception.ResponseConverterException;
import com.google.common.collect.Range;

public class ResultOrErrorProcessor<T, E> {

  private ResultProcessor<T> responseProcessor;
  private TypeConverter<E> errorConverter;
  private ru.hh.jclient.common.converter.TypeConverter<E> oldErrorConverter;
  private Range<Integer> errorsRange = Range.greaterThan(OK_RANGE.upperEndpoint());

  ResultOrErrorProcessor(ResultProcessor<T> responseProcessor, TypeConverter<E> errorConverter) {
    this.responseProcessor = requireNonNull(responseProcessor, "responseProcessor must not be null");
    this.errorConverter = requireNonNull(errorConverter, "errorConverter must not be null");
  }

  ResultOrErrorProcessor(ResultProcessor<T> responseProcessor, ru.hh.jclient.common.converter.TypeConverter<E> errorConverter) {
    this.responseProcessor = requireNonNull(responseProcessor, "responseProcessor must not be null");
    this.oldErrorConverter = requireNonNull(errorConverter, "errorConverter must not be null");
  }

  /**
   * Specifies HTTP status code that is eligible for ERROR response parsing. It must not intersect with {@link HttpClient#OK_RANGE}.
   *
   * @param status HTTP status code that converter will be used for
   */
  public ResultOrErrorProcessor<T, E> forStatus(int status) {
    return forStatus(Range.singleton(status));
  }

  /**
   * Specifies range of HTTP status codes that are eligible for ERROR response parsing. It must not intersect with {@link HttpClient#OK_RANGE}.
   *
   * @param status HTTP status codes that converter will be used for
   */
  public ResultOrErrorProcessor<T, E> forStatus(Range<Integer> statusCodes) {
    if (OK_RANGE.isConnected(statusCodes)) {
      throw new IllegalArgumentException(String.format("Statuses %s are intersect with non-error statuses", statusCodes.toString()));
    }
    this.errorsRange = statusCodes;
    return this;
  }

  /**
   * Returns future containing wrapper that consists of:
   * <ul>
   * <li>expected result, if HTTP status code is in {@link HttpClient#OK_RANGE}, otherwise {@link Optional#empty()}</li>
   * <li>error result, if HTTP status code is NOT in {@link HttpClient#OK_RANGE}, otherwise {@link Optional#empty()}</li>
   * <li>response object</li>
   * </ul>
   *
   * By default ERROR result will be parsed if HTTP status code is not in {@link HttpClient#OK_RANGE}. More specific range can be specified using
   * {@link #forStatus(Range)} method. Once called, any errors not in that range will NOT be parsed and can be handled manually.
   *
   * @return {@link ResultOrErrorWithResponse} object with results of response processing
   * @throws ResponseConverterException if failed to process response with either normal or error converter
   */
  public CompletableFuture<ResultOrErrorWithResponse<T, E>> resultWithResponse() {
    return responseProcessor.getHttpClient().unconverted().thenApply(this::wrapResponseAndError);
  }

  /**
   * Returns future containing wrapper that consists of:
   * <ul>
   * <li>expected result, if HTTP status code is in {@link HttpClient#OK_RANGE}, otherwise {@link Optional#empty()}</li>
   * <li>error result, if HTTP status code is NOT in {@link HttpClient#OK_RANGE}, otherwise {@link Optional#empty()}</li>
   * <li>response status code</li>
   * </ul>
   *
   * By default ERROR result will be parsed if HTTP status code is not in {@link HttpClient#OK_RANGE}. More specific range can be specified using
   * {@link #forStatus(Range)} method. Once called, any errors not in that range will NOT be parsed and can be handled manually.
   *
   * @return {@link ResultOrErrorWithStatus} object with results of response processing
   * @throws ResponseConverterException if failed to process response with either normal or error converter
   */
  public CompletableFuture<ResultOrErrorWithStatus<T, E>> resultWithStatus() {
    return resultWithResponse().thenApply(ResultOrErrorWithResponse::hideResponse);
  }

  private ResultOrErrorWithResponse<T, E> wrapResponseAndError(Response response) {
    Optional<T> value;
    Optional<E> errorValue;
    try {
      if (HttpClient.OK_RESPONSE.apply(response)) {
        if (responseProcessor.getConverter() != null) {
          value = responseProcessor.getConverter().converterFunction().apply(response).get();
        }
        else {
          value = responseProcessor.getOldConverter().converterFunction().apply(response.getDelegate()).get();
        }

        errorValue = Optional.empty();

        responseProcessor.getHttpClient().getDebug().onResponseConverted(value);
      }
      else {
        value = Optional.empty();
        errorValue = parseError(response);

        responseProcessor.getHttpClient().getDebug().onResponseConverted(errorValue);
      }
      return new ResultOrErrorWithResponse<>(value, errorValue, response);
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
      if (errorConverter != null) {
        return errorConverter.converterFunction().apply(response).get();
      }
      return oldErrorConverter.converterFunction().apply(response.getDelegate()).get();
    }
    return Optional.empty();
  }

}
