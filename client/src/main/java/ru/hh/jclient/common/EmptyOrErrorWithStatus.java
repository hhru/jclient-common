package ru.hh.jclient.common;

import javax.annotation.Nullable;
import ru.hh.jclient.common.errors.check.ApplyEmptyResultOperationSelector;
import ru.hh.jclient.common.errors.check.ApplyEmptyResultOrErrorOperationSelector;

public class EmptyOrErrorWithStatus<E> extends ResultOrErrorWithStatus<Void, E> {

  public EmptyOrErrorWithStatus(@Nullable E errorValue, int statusCode) {
    super(null, errorValue, statusCode);
  }

  public ApplyEmptyResultOrErrorOperationSelector<E> checkEmptyErrorResult(
      String errorMessage,
      Object... errorMessageParams) {
    return new ApplyEmptyResultOrErrorOperationSelector<>(this, errorMessage, errorMessageParams);
  }

  public ApplyEmptyResultOperationSelector checkEmptyResult(String errorMessage, Object... errorMessageParams) {
    return new ApplyEmptyResultOperationSelector(checkEmptyErrorResult(errorMessage, errorMessageParams)
        .returnEmpty().onAnyError(), errorMessage, errorMessageParams);
  }
}
