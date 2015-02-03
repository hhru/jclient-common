package ru.hh.jclient.common;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import ru.hh.jclient.common.exception.ResponseConverterException;

import com.ning.http.client.Response;

public class ResponseAndErrorProcessor<T, E> {

  private ResponseProcessor<T> responseProcessor;
  private AbstractConverter<E> errorConverter;

  ResponseAndErrorProcessor(ResponseProcessor<T> responseProcessor, AbstractConverter<E> errorConverter) {
    this.responseProcessor = responseProcessor;
    this.errorConverter = errorConverter;
  }

  public CompletableFuture<ResponseAndErrorWrapper<T, E>> request() {
    return responseProcessor.uncheckedRequest().thenApply(this::responseAndErrorWrapper);
  }

  private ResponseAndErrorWrapper<T, E> responseAndErrorWrapper(Response response) {
    Optional<T> value;
    Optional<E> errorValue;
    try {
      if (HttpClient.OK_RESPONSE.apply(response)) {
        errorValue = Optional.empty();
        value = Optional.of(responseProcessor.getConverter().converterFunction().apply(response).get());
      }
      else {
        errorValue = Optional.of(errorConverter.converterFunction().apply(response).get());
        value = Optional.empty();
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
