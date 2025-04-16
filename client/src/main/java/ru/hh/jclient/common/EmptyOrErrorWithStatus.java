package ru.hh.jclient.common;

import jakarta.annotation.Nullable;

public class EmptyOrErrorWithStatus<E> extends ResultOrErrorWithStatus<Void, E> {

  public EmptyOrErrorWithStatus(@Nullable E errorValue, int statusCode) {
    super(null, errorValue, statusCode);
  }
}
