package ru.hh.jclient.errors.impl.check;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import ru.hh.jclient.common.EmptyOrErrorWithStatus;
import ru.hh.jclient.common.ResultOrErrorWithStatus;
import ru.hh.jclient.errors.impl.PredicateWithStatus;
import static java.util.Optional.empty;

public class ApplyEmptyResultOrErrorOperationSelector<E>
    extends AbstractApplyResultOrErrorOperationSelector<Void, ApplyEmptyResultOrErrorOperationSelector<E>, ApplyEmptyResultOrErrorOperation<E>, E> {

  public ApplyEmptyResultOrErrorOperationSelector(ResultOrErrorWithStatus<Void, E> resultOrErrorWithStatus, String errorMessage, Object... params) {
    super(resultOrErrorWithStatus, errorMessage, params);
  }

  @Override
  protected ApplyEmptyResultOrErrorOperation<E> createOperation(ResultOrErrorWithStatus<Void, E> wrapper,
      Optional<Integer> errorStatusCode,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<E>> predicates,
      Optional<Void> defaultValue) {
    return new ApplyEmptyResultOrErrorOperation<>((EmptyOrErrorWithStatus<E>) wrapper, errorStatusCode, errorMessage, predicates, false);
  }

  /**
   * <p>
   * Specifies empty value to return if error is present.
   * </p>
   */
  public ApplyEmptyResultOrErrorOperation<E> returnEmpty() {
    return new ApplyEmptyResultOrErrorOperation<>((EmptyOrErrorWithStatus<E>) resultOrErrorWithStatus, empty(), errorMessage, predicates, true);
  }

}
