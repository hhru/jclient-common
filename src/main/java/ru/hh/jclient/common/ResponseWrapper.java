package ru.hh.jclient.common;

import com.ning.http.client.Response;

public class ResponseWrapper<T> {

  private T value;
  private Response response;

  public ResponseWrapper(T value, Response response) {
    this.value = value;
    this.response = response;
  }

  public T get() {
    return value;
  }

  public Response getResponse() {
    return response;
  }

}
