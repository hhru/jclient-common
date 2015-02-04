package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;
import com.ning.http.client.Response;

public class ResponseWrapper<T> {

  private T value;
  private Response response;

  public ResponseWrapper(T value, Response response) {
    this.value = value; // can be null
    this.response = requireNonNull(response, "response must not be null");
  }

  public T get() {
    return value;
  }

  public Response getResponse() {
    return response;
  }

}
