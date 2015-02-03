package ru.hh.jclient.common;

import java.util.Optional;

import com.ning.http.client.Response;

public class ResponseAndErrorWrapper<T, E> {

  private Optional<T> value;
  private Optional<E> errorValue;
  private Response response;

  ResponseAndErrorWrapper(Optional<T> value, Optional<E> errorValue, Response response) {
    this.value = value;
    this.errorValue = errorValue;
    this.response = response;
  }

  public Optional<T> get() {
    return value;
  }

  public Optional<E> getError() {
    return errorValue;
  }

  public Response getResponse() {
    return response;
  }

  public boolean isSuccess() {
    return !errorValue.isPresent();
  }

}
