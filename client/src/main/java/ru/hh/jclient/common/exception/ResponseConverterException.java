package ru.hh.jclient.common.exception;

public class ResponseConverterException extends RuntimeException {

  public ResponseConverterException(String message, Throwable cause) {
    super(message, cause);
  }

  public ResponseConverterException(String message) {
    super(message);
  }

  public ResponseConverterException(Throwable cause) {
    super(cause);
  }

}
