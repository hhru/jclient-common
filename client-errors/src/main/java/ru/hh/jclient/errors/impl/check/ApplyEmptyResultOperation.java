package ru.hh.jclient.errors.impl.check;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.ws.rs.WebApplicationException;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.impl.ExceptionBuilder;
import ru.hh.jclient.errors.impl.PredicateWithStatus;

public class ApplyEmptyResultOperation extends AbstractOperation<Void, ApplyEmptyResultOperation> {

  public ApplyEmptyResultOperation(
      ResultWithStatus<Void> wrapper,
      Optional<Integer> errorStatusCode,
      Optional<List<Integer>> proxiedStatusCodes,
      Optional<Function<Integer, Integer>> statusCodesConverter,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<Void>> predicates,
      Set<Integer> allowedStatuses,
      ExceptionBuilder<?, ?> exceptionBuilder) {
    super(wrapper, errorStatusCode, proxiedStatusCodes, statusCodesConverter, errorMessage, predicates, allowedStatuses, exceptionBuilder);
  }

  public ApplyEmptyResultOperation(
      ResultWithStatus<Void> wrapper,
      Optional<Integer> errorStatusCode,
      Optional<List<Integer>> proxiedStatusCodes,
      Optional<Function<Integer, Integer>> statusCodesConverter,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<Void>> predicates,
      Optional<Void> defaultValue,
      Set<Integer> allowedStatuses,
      ExceptionBuilder<?, ?> exceptionBuilder) {
    super(wrapper, errorStatusCode, proxiedStatusCodes, statusCodesConverter, errorMessage,
        predicates, defaultValue, allowedStatuses, exceptionBuilder);
  }

  /**
   * <p>
   * Returns result or throws {@link WebApplicationException} with provided status code on status code error - if {@link ResultWithStatus#isSuccess()}
   * is false.
   * </p>
   * <p>
   * If default value is specified, it will be returned instead of exception.
   * </p>
   *
   * @throws WebApplicationException
   *           with provided status code and message in case of error (if default value is not specified)
   * @return result or default value (if specified) in case of error
   */
  public Optional<Void> onStatusCodeError() {
    return checkForStatusCodeError();
  }

  public CheckedResultWithStatus<Void> onStatusCodeErrorWrapped() {
    checkForStatusCodeError();
    return new CheckedResultWithStatus<>(wrapper);
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
  public Optional<Void> onPredicate() {
    return checkForPredicates(wrapper.get());
  }

  public CheckedResultWithStatus<Void> onPredicateWrapped() {
    checkForPredicates(wrapper.get());
    return new CheckedResultWithStatus<>(wrapper);
  }
}
