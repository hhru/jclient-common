package ru.hh.jclient.common;

import javax.annotation.Nullable;

public class EmptyOrErrorWithStatus<E> extends ResultOrErrorWithStatus<Void, E> {

  public EmptyOrErrorWithStatus(@Nullable E errorValue, int statusCode) {
    super(null, errorValue, statusCode);
  }
}
