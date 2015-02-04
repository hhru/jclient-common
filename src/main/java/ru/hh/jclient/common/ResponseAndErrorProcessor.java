package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import ru.hh.jclient.common.converter.TypeConverter;
import ru.hh.jclient.common.exception.ResponseConverterException;
import com.ning.http.client.Response;

public class ResponseAndErrorProcessor<T, E> {

  private ResponseProcessor<T> responseProcessor;
  private TypeConverter<E> errorConverter;

  ResponseAndErrorProcessor(ResponseProcessor<T> responseProcessor, TypeConverter<E> errorConverter) {
    this.responseProcessor = requireNonNull(responseProcessor, "responseProcessor must not be null");
    this.errorConverter = requireNonNull(errorConverter, "errorConverter must not be null");
  }

  /**
   * Returns wrapper consisting of:
   * <ul>
   * <li>expected result, if status code is in {@link HttpClient#OK_RANGE}, otherwise {@link Optional#empty()}</li>
   * <li>error result, if status code is NOT in {@link HttpClient#OK_RANGE}, otherwise {@link Optional#empty()}</li>
   * <li>response object</li>
   * </ul>
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
        value = Optional.of(responseProcessor.getConverter().converterFunction().apply(response).get());
        errorValue = Optional.empty();
      }
      else {
        value = Optional.empty();
        errorValue = Optional.of(errorConverter.converterFunction().apply(response).get());
      }
      return new ResponseAndErrorWrapper<T, E>(value, errorValue, response);
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

}
