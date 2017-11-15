package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;

/**
 * Wrapper object that contains {@link Response} object and the result of conversion. It is not recommended to use this wrapper outside the client
 * code to ensure ning encapsulation.
 *
 * @param <T> type of conversion result
 */
public class ResultWithResponse<T> extends ResultWithStatus<T> {

  private Response response;

  public ResultWithResponse(T value, com.ning.http.client.Response response) {
    this(value, new Response(response));
  }

  public ResultWithResponse(T value, Response response) {
    super(value, response.getStatusCode());
    this.response = requireNonNull(response, "response must not be null");
  }

  /**
   * @return response object
   */
  public Response unconverted() {
    return response;
  }

  /**
   * @deprecated use {@link #unconverted()}
   */
  @Deprecated
  public com.ning.http.client.Response getResponse() {
    return response.getDelegate();
  }

  /**
   * @return this wrapper cast to {@link ResultWithStatus<T>}
   */
  public ResultWithStatus<T> hideResponse() {
    return this;
  }
}
