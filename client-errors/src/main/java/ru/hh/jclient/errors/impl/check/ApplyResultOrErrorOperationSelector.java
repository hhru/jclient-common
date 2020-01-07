package ru.hh.jclient.errors.impl.check;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import ru.hh.jclient.common.ResultOrErrorWithStatus;
import ru.hh.jclient.errors.impl.PredicateWithStatus;

public class ApplyResultOrErrorOperationSelector<T, E>
    extends AbstractApplyResultOrErrorOperationSelector<T, ApplyResultOrErrorOperationSelector<T, E>, ApplyResultOrErrorOperation<T, E>, E> {

  public ApplyResultOrErrorOperationSelector(ResultOrErrorWithStatus<T, E> resultOrErrorWithStatus, String errorMessage, Object... params) {
    super(resultOrErrorWithStatus, errorMessage, params);
  }

  @Override
  protected ApplyResultOrErrorOperation<T, E> createOperation(ResultOrErrorWithStatus<T, E> wrapper,
      Optional<Integer> errorStatusCode,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<E>> predicates,
      Optional<T> defaultValue) {
    return new ApplyResultOrErrorOperation<>(wrapper, errorStatusCode, errorMessage, predicates, defaultValue);
  }

  /**
   * Specifies default value to set to result if error is present.
   *
   * @param defaultValue
   *          default value to set
   */
  public ApplyResultOrErrorOperation<T, E> setDefault(T defaultValue) {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, empty(), errorMessage, predicates, of(defaultValue));
  }
  /**
   * Specifies default value to set to result if error is present.
   *
   * @param defaultValue
   *          default value to set
   */
  public ApplyResultOrErrorOperation<T, E> setDefault(Supplier<T> defaultValue) {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, empty(), errorMessage, predicates, of(defaultValue.get()));
  }
}
