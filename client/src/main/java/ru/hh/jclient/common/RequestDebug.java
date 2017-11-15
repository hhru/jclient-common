package ru.hh.jclient.common;

import java.util.Optional;
import ru.hh.jclient.common.exception.ResponseConverterException;
import com.ning.http.client.AsyncHttpClientConfig;

/**
 * Describes object used to gather debug information on performing local (outgoing) request.
 */
public interface RequestDebug {

  default void onRequest(AsyncHttpClientConfig config, Request request, Optional<?> requestBodyEntity) {
    onRequest(config, request.getDelegate(), requestBodyEntity);
  }

  /**
   * Called before start of the request
   *
   * @deprecated use {@link #onRequest(AsyncHttpClientConfig, Request, Optional)}
   */
  @Deprecated
  void onRequest(AsyncHttpClientConfig config, com.ning.http.client.Request request, Optional<?> requestBodyEntity);

  default void onRetry(AsyncHttpClientConfig config, Request request, Optional<?> requestBodyEntity, int retryCount, String upstreamName) {
    onRetry(config, request.getDelegate(), requestBodyEntity, retryCount, upstreamName);
  }

  /**
   * Called before retrying the request
   *
   * @deprecated use {@link #onRetry(AsyncHttpClientConfig, Request, Optional, int, String)}
   */
  @Deprecated
  void onRetry(
      AsyncHttpClientConfig config,
      com.ning.http.client.Request request,
      Optional<?> requestBodyEntity,
      int retryCount,
      String upstreamName);

  default Response onResponse(AsyncHttpClientConfig config, Response response) {
    return new Response(onResponse(config, response.getDelegate()));
  }

  /**
   * Called once response is fully parsed. Returned response will be used for further processing, so there is ability to replace it for debug
   * purposes.
   *
   * @deprecated use {@link #onResponse(AsyncHttpClientConfig, Response)}
   */
  @Deprecated
  com.ning.http.client.Response onResponse(AsyncHttpClientConfig config, com.ning.http.client.Response response);

  /**
   * Called once response is successfully converted.
   *
   * @param result result of response conversion
   */
  void onResponseConverted(Optional<?> result);

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

  /**
   * Adds label for debug purposes.
   */
  void addLabel(String label);

}
