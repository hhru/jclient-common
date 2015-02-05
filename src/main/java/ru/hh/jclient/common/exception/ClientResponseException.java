package ru.hh.jclient.common.exception;

import com.ning.http.client.Response;

public class ClientResponseException extends RuntimeException {

  private Response response;

  public ClientResponseException(Response response) {
    this.response = response;
  }

  public ClientResponseException(Response response, String message) {
    super(message);
    this.response = response;
  }

  public Response getResponse() {
    return response;
  }

}
