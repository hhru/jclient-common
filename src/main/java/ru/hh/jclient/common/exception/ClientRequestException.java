package ru.hh.jclient.common.exception;

import com.ning.http.client.Response;

public class ClientRequestException extends RuntimeException {

  private Response response;

  public ClientRequestException(Response response) {
    this.response = response;
  }

  public Response getResponse() {
    return response;
  }

}
