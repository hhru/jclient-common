package ru.hh.jclient.common.exception;

import static java.lang.String.format;
import com.ning.http.client.Response;

public class ClientResponseException extends RuntimeException {

  private Response response;

  public ClientResponseException(Response response) {
    super(response.toString());
    this.response = response;
  }

  public ClientResponseException(Response response, String message) {
    super(format("%s\n%s", message, response.toString()));
    this.response = response;
  }

  public int getStatusCode() {
    return response.getStatusCode();
  }

}
