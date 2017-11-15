package ru.hh.jclient.common.exception;

import static java.lang.String.format;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.util.ResponseUtils;

public class ClientResponseException extends RuntimeException {

  private Response response;

  public ClientResponseException(Response response) {
    super(ResponseUtils.toString(response));
    this.response = response;
  }

  public ClientResponseException(Response response, String message) {
    super(format("%s\n%s", message, ResponseUtils.toString(response)));
    this.response = response;
  }

  public int getStatusCode() {
    return response.getStatusCode();
  }

}
