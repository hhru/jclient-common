package ru.hh.jclient.common.errors.check;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.ResultOrErrorWithStatus;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.common.errors.OperationBase;
import ru.hh.jclient.common.errors.PredicateWithStatus;

public class ApplyResultOrErrorOperation<T, E> extends OperationBase<ApplyResultOrErrorOperation<T, E>> {

  protected static final Logger logger = LoggerFactory.getLogger(ApplyResultOrErrorOperation.class);

  protected ResultOrErrorWithStatus<T, E> wrapper;

  protected List<PredicateWithStatus<E>> predicates;
  @Nullable
  protected T defaultValue;
  private boolean useDefault = false;

  public ApplyResultOrErrorOperation(
      ResultOrErrorWithStatus<T, E> wrapper,
      @Nullable Integer errorStatusCode,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<E>> predicates,
      @Nullable T defaultValue
  ) {
    this(wrapper, errorStatusCode, errorMessage, predicates);
    this.defaultValue = defaultValue;
    this.useDefault = true;
  }

  public ApplyResultOrErrorOperation(
      ResultOrErrorWithStatus<T, E> wrapper,
      @Nullable Integer errorStatusCode,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<E>> predicates
  ) {
    super(
        AbstractOperation.getStatusCodeIfAbsent(wrapper, errorStatusCode, List.of(), null).orElse(null),
        wrapper.getStatusCode(),
        errorMessage
    );
    this.wrapper = wrapper;
    this.predicates = predicates;
  }

  /**
   * <p>
   * Returns result or throws {@link WebApplicationException} with provided status code on any error including:
   * <ul>
   * <li>{@link ResultOrErrorWithStatus#isSuccess()} is false</li>
   * <li>predicate provided with {@link AbstractOperationSelector#failIf(java.util.function.Predicate)} says ResultWithStatus contains incorrect value
   * </li>
   * <li>{@link ResultOrErrorWithStatus#uncheckedResult()} contains {@link Optional#empty()}</li>
   * </ul>
   * </p>
   * <p>
   * If default value is specified, it will be returned instead of exception.
   * </p>
   *
   * @throws WebApplicationException
   *           with provided status code and message in case of error (if default value is not specified)
   * @return unwrapped non null result or default value (if specified) in case of error
   */
  public ResultWithStatus<T> onAnyError() {
    if (wrapper.getError().isEmpty()) {
      return wrapper;
    }
    checkForPredicates(wrapper.getError().get()); // set proper status if matched
    return defaultOrThrow("error is not empty");
  }

  protected void checkForPredicates(E error) {
    predicates.forEach(p -> testPredicate(p, error));
  }

  private void testPredicate(PredicateWithStatus<E> predicate, E error) {
    if (predicate.getPredicate().test(error)) {
      this.errorStatusCode = predicate.getStatus().orElse(errorStatusCode);
    }
  }

  protected ResultWithStatus<T> defaultOrThrow(String cause) {
    if (useDefault) {
      logger.warn("Default value is set to result because error happened: {}. Description: {}", cause, exceptionBuilder.getMessage());
      return new ResultWithStatus<>(defaultValue, wrapper.getStatusCode());
    }
    throw toException(cause);
  }

  // chaining setter methods

  /**
   * Same as {@link #as(BiFunction)}} but accepts function that knows about Error instance for better error creation.
   */
  public ApplyResultOrErrorOperation<T, E> as(Function<E, BiFunction<String, Integer, Object>> errorEntityCreator) {
    return super.as(() -> errorEntityCreator.apply(wrapper.getError().get()));
  }

}
