package ru.hh.jclient.common;

import jakarta.annotation.Nullable;
import ru.hh.jclient.common.exception.ResponseConverterException;
import ru.hh.jclient.common.responseconverter.TypeConverter;

/**
 * Listener for outgoing requests that can be used both for gathering debug information and for fast-fail checks
 * (e.g. global timeout / deadline checks).
 *
 * <p>Previously this functionality was provided by {@code RequestDebug}. The interface was renamed to
 * {@code HttpClientEventListener}. All existing implementations can migrate by simply renaming the reference.</p>
 */
public interface HttpClientEventListener {
  HttpClientEventListener DISABLED_EVENT_LISTENER = new HttpClientEventListener(){};

  /** Called before the request is executed by the underlying HTTP client. */
  default void beforeExecute(HttpClient httpClient, Request request) {
  }

  /** Called before starting the request. */
  default void onRequest(Request request, @Nullable Object requestBodyEntity, RequestContext context) {
  }

  /** Called before retrying the request. */
  default void onRetry(Request request, @Nullable Object requestBodyEntity, int retryCount, RequestContext context) {
  }

  /** Called once response is fully parsed; may return modified response. */
  default Response onResponse(Response response) {
    return response;
  }

  /** Indicates if {@link #onResponse(Response)} can unwrap debug envelope. */
  default boolean canUnwrapDebugResponse() { 
    return false; 
  }

  /** Called when response successfully converted to result. */
  default void onResponseConverted(@Nullable Object result) {
  }

  /** Called on problem raised by underlying http client. */
  default void onClientProblem(Throwable t) {
  }

  /** Called on problem raised while converting response. */
  default void onConverterProblem(ResponseConverterException e) {
  }

  /** Called after response processing finished. */
  default void onProcessingFinished() {
  }

  /** Hook invoked on creating result processor. */
  default <T> void onCreateResultProcessor(HttpClient httpClient, TypeConverter<T> converter) {
  }
}

