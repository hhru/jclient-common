package ru.hh.jclient.common;

import java.util.Optional;
import static java.util.Optional.empty;

public class EmptyOrErrorWithStatus<E> extends ResultOrErrorWithStatus<Void, E> {

  public EmptyOrErrorWithStatus(Optional<E> errorValue, int statusCode) {
    super(empty(), errorValue, statusCode);
  }
}
