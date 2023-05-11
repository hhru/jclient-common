package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;
import javax.annotation.Nullable;

/**
 * Wrapper object that contains {@link Response} object and either normal or ERROR result of conversion. It is not recommended to use this wrapper
 * outside the client code to ensure ning encapsulation.
 *
 * @param <T> type of conversion result
 * @param <E> type of ERROR conversion result
 */
public class ResultOrErrorWithResponse<T, E> extends ResultOrErrorWithStatus<T, E> {

  private Response response;

  public ResultOrErrorWithResponse(@Nullable T value, @Nullable E errorValue, org.asynchttpclient.Response response) {
    this(value, errorValue, new Response(response));
  }

  public ResultOrErrorWithResponse(@Nullable T value, @Nullable E errorValue, Response response) {
    super(value, errorValue, response.getStatusCode());
    this.response = requireNonNull(response, "response must not be null");
  }

  /**
   * @return response object
   */
  public Response getResponse() {
    return response;
  }

  /**
   * @return this wrapper cast to {@link ResultOrErrorWithStatus}
   */
  public ResultOrErrorWithStatus<T, E> hideResponse() {
    return this;
  }
}
