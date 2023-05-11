package ru.hh.jclient.common;

import javax.annotation.Nullable;
import ru.hh.jclient.common.exception.ResponseConverterException;
import ru.hh.jclient.common.responseconverter.TypeConverter;

/**
 * Describes object used to gather debug information on performing local (outgoing) request.
 */
public interface RequestDebug {

  RequestDebug DISABLED = new RequestDebug() {
    @Override
    public void onRequest(Request request, @Nullable Object requestBodyEntity, RequestContext context) {
    }

    @Override
    public void onRetry(Request request, @Nullable Object requestBodyEntity, int retryCount, RequestContext context) {
    }

    @Override
    public Response onResponse(Response response) {
      return response;
    }

    @Override
    public boolean canUnwrapDebugResponse() {
      return false;
    }

    @Override
    public void onResponseConverted(@Nullable Object result) {
    }

    @Override
    public void onClientProblem(Throwable t) {
    }

    @Override
    public void onConverterProblem(ResponseConverterException e) {
    }

    @Override
    public void onProcessingFinished() {
    }
  };

  /**
   * Called before start of the request
   */
  void onRequest(Request request, @Nullable Object requestBodyEntity, RequestContext context);

  /**
   * Called before retrying the request
   */
  void onRetry(Request request, @Nullable Object requestBodyEntity, int retryCount, RequestContext context);

  /**
   * Called once response is fully parsed. Returned response will be used for further processing, so there is ability to replace it for debug
   * purposes.
   */
  Response onResponse(Response response);

  /**
   * Tells if {@link #onResponse(Response)} can extract actual response from a debug response envelope
   * (see {@link HttpHeaderNames#X_HH_DEBUG})
   */
  default boolean canUnwrapDebugResponse() {
    return false;
  }

  /**
   * Called once response is successfully converted.
   *
   * @param result result of response conversion
   */
  void onResponseConverted(@Nullable Object result);

  /**
   * Called on the problem raised by underlying http client.
   */
  void onClientProblem(Throwable t);

  /**
   * Called on the problem raised while converting response to result object.
   */
  void onConverterProblem(ResponseConverterException e);

  /**
   * Called after response processing has been finished.
   */
  void onProcessingFinished();

  default <T> void onCreateResultProcessor(HttpClient httpClient, TypeConverter<T> converter) {
  }
}
