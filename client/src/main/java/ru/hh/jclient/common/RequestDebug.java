package ru.hh.jclient.common;

import java.util.Optional;
import ru.hh.jclient.common.exception.ResponseConverterException;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

/**
 * Describes object used to gather debug information on performing local (outgoing) request.
 */
public interface RequestDebug {

  /**
   * Called before start of the request
   */
  void onRequest(AsyncHttpClientConfig config, Request request, Optional<?> requestBodyEntity);

  /**
   * Called before retrying the request
   */
  void onRetry(AsyncHttpClientConfig config, Request request, Optional<?> requestBodyEntity, int retryCount, String upstreamName);

  /**
   * Called once response is fully parsed. Returned response will be used for further processing, so there is ability to replace it for debug
   * purposes.
   */
  Response onResponse(AsyncHttpClientConfig config, Response response);

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
