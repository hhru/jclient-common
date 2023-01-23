package ru.hh.jclient.errors;

import ru.hh.jclient.errors.impl.ExceptionBuilder;

public class CustomExceptionBuilder extends ExceptionBuilder<CustomException, CustomExceptionBuilder> {
  @Override
  public CustomException toException() {
    return new CustomException();
  }
}
