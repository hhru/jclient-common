package ru.hh.jclient.common.exception;

import ru.hh.jclient.common.Response;

public class NoContentTypeException extends ClientResponseException {

  public NoContentTypeException(Response response) {
    super(response);
  }

}
