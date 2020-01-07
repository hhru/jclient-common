package ru.hh.jclient.common;

import java.util.Optional;

public class EmptyWithStatus extends ResultWithStatus<Void> {

  public EmptyWithStatus(int statusCode) {
    super(null, statusCode);
  }

  /**
   * Always returns empty Optional
   */
  @Override
  public Optional<Void> get() {
    return Optional.empty();
  }
}
