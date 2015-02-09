package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import com.ning.http.client.Response;

/**
 * Wrapper object that contains {@link Response} object and result of conversion.
 * 
 * @param <T> type of conversion result
 */
public class ResponseWrapper<T> {

  private Optional<T> value;
  private Response response;

  public ResponseWrapper(T value, Response response) {
    this.value = Optional.ofNullable(value);
    this.response = requireNonNull(response, "response must not be null");
  }

  /**
   * Returns result of conversion.
   * 
   * @return result
   */
  public Optional<T> get() {
    return value;
  }

  public Response getResponse() {
    return response;
  }

}
