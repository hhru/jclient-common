package ru.hh.jclient.common.exception;

import com.ning.http.client.Response;

public class NoContentTypeException extends ClientResponseException {

  public NoContentTypeException(Response response) {
    super(response);
  }

}
