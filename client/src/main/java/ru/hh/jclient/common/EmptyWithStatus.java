package ru.hh.jclient.common;

import java.util.Optional;
import ru.hh.jclient.common.errors.check.ApplyEmptyResultOperationSelector;

public class EmptyWithStatus extends ResultWithStatus<Void> {

  public EmptyWithStatus(int statusCode) {
    super(null, statusCode);
  }

  /**
   * Always returns empty Optional
   */
  @Override
  public Optional<Void> uncheckedResult() {
    return Optional.empty();
  }

  public ApplyEmptyResultOperationSelector checkEmptyResult(String errorMessage, Object... errorMessageParams) {
    return new ApplyEmptyResultOperationSelector(this, errorMessage, errorMessageParams);
  }

}
