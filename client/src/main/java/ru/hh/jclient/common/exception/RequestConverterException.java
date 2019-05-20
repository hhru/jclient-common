package ru.hh.jclient.common.exception;

public class RequestConverterException extends RuntimeException {

  public RequestConverterException(String message, Throwable cause) {
    super(message, cause);
  }

  public RequestConverterException(String message) {
    super(message);
  }

  public RequestConverterException(Throwable cause) {
    super(cause);
  }

}
