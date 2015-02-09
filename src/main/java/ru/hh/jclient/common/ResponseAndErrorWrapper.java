package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;
import java.util.Optional;
import com.ning.http.client.Response;

public class ResponseAndErrorWrapper<T, E> {

  private Optional<T> value;
  private Optional<E> errorValue;
  private Response response;

  public ResponseAndErrorWrapper(Optional<T> value, Optional<E> errorValue, Response response) {
    this.value = requireNonNull(value, "value must not be null");
    this.errorValue = requireNonNull(errorValue, "errorValue must not be null");
    this.response = requireNonNull(response, "response must not be null");
  }

  /**
   * Returns result of response conversion. Can be {@link Optional#empty() empty} if error has happened.
   */
  public Optional<T> get() {
    return value;
  }

  /**
   * Returns result of ERROR response conversion. {@link Optional#empty() Empty} if error did not happen.
   */
  public Optional<E> getError() {
    return errorValue;
  }

  /**
   * Returns response object.
   */
  public Response getResponse() {
    return response;
  }

  /**
   * Returns true if error value is not present.
   */
  public boolean isSuccess() {
    return !errorValue.isPresent();
  }

}
