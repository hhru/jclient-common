package ru.hh.jclient.errors.impl.check;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.impl.ExceptionBuilder;
import ru.hh.jclient.errors.impl.PredicateWithStatus;

/**
 * Contains useful methods to handle error outcome of {@link CompletableFuture} with different jclient-common wrappers.
 */
public class ApplyResultOperation<T> extends AbstractOperation<T, ApplyResultOperation<T>> {

  // constructors

  public ApplyResultOperation(
      ResultWithStatus<T> wrapper,
      @Nullable Integer errorStatusCode,
      List<Integer> proxiedStatusCodes,
      @Nullable Function<Integer, Integer> statusCodesConverter,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<T>> predicates,
      Set<Integer> allowStatuses,
      ExceptionBuilder<?, ?> exceptionBuilder) {
    super(
        wrapper,
        errorStatusCode,
        proxiedStatusCodes,
        statusCodesConverter,
        errorMessage,
        predicates,
        allowStatuses,
        exceptionBuilder
    );
  }

  public ApplyResultOperation(
      ResultWithStatus<T> wrapper,
      @Nullable Integer errorStatusCode,
      List<Integer> proxiedStatusCodes,
      @Nullable Function<Integer, Integer> statusCodesConverter,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<T>> predicates,
      @Nullable T defaultValue,
      Set<Integer> allowStatuses,
      ExceptionBuilder<?, ?> exceptionBuilder) {
    super(
        wrapper,
        errorStatusCode,
        proxiedStatusCodes,
        statusCodesConverter,
        errorMessage,
        predicates,
        defaultValue,
        allowStatuses,
        exceptionBuilder
    );
  }

  // terminal operations

  /**
   * <p>
   * Returns result or throws {@link WebApplicationException} with provided status code on any error including:
   * <ul>
   * <li>{@link ResultWithStatus#isSuccess()} is false</li>
   * <li>predicate provided with {@link AbstractOperationSelector#failIf(java.util.function.Predicate)} says ResultWithStatus contains incorrect value
   * </li>
   * <li>{@link ResultWithStatus#get()} contains {@link Optional#empty()} if status is not allowed</li>
   * </ul>
   * </p>
   * <p>
   * If default value is specified, it will be returned instead of exception.
   * </p>
   *
   * @throws WebApplicationException
   *           with provided status code and message in case of error (if default value is not specified)
   * @return unwrapped non null (except default is null) result or default value (if specified) in case of error
   */
  public T onAnyError() {
    return checkForAnyError().orElse(null);
  }

  public CheckedResultWithStatus<T> onAnyErrorWrapped() {
    return new CheckedResultWithStatus<>(checkForAnyError().orElse(null), wrapper.getStatusCode());
  }

  /**
   * <p>
   * Returns result or throws {@link WebApplicationException} with provided status code if {@link ResultWithStatus#get()} contains
   * {@link Optional#empty()}.
   * </p>
   * <p>
   * If default value is specified, it will be returned instead of exception.
   * </p>
   *
   * @throws WebApplicationException
   *           with provided status code and message in case of error (if default value is not specified)
   * @return unwrapped non null (except default is null) result or default value (if specified) in case of error
   */
  public T onEmpty() {
    return checkForEmpty().orElse(null);
  }

  public CheckedResultWithStatus<T> onEmptyWrapped() {
    return new CheckedResultWithStatus<>(checkForEmpty().orElse(null), wrapper.getStatusCode());
  }

  /**
   * <p>
   * Returns result or throws {@link WebApplicationException} with provided status code on status code error - if {@link ResultWithStatus#isSuccess()}
   * is false, except allowed statuses.
   * </p>
   * <p>
   * If default value is specified, it will be returned instead of exception.
   * </p>
   *
   * @throws WebApplicationException
   *           with provided status code and message in case of error (if default value is not specified)
   * @return result or default value (if specified) in case of error
   */
  public Optional<T> onStatusCodeError() {
    return checkForStatusCodeError();
  }

  public CheckedResultWithStatus<T> onStatusCodeErrorWrapped() {
    return new CheckedResultWithStatus<>(checkForStatusCodeError().orElse(null), wrapper.getStatusCode());
  }

  /**
   * <p>
   * Returns result or throws {@link WebApplicationException} with provided status code if predicate specified with
   * {@link AbstractOperationSelector#failIf(java.util.function.Predicate)} returns 'true'.
   * </p>
   * <p>
   * If default value is specified, it will be returned instead of exception.
   * </p>
   *
   * @throws WebApplicationException
   *           with provided status code and message in case of error (if default value is not specified)
   * @return result or default value (if specified) in case of error
   */
  public Optional<T> onPredicate() {
    return checkForPredicates(wrapper.get().orElse(null));
  }

  public CheckedResultWithStatus<T> onPredicateWrapped() {
    return new CheckedResultWithStatus<>(checkForPredicates(wrapper.get().orElse(null)).orElse(null), wrapper.getStatusCode());
  }
}
