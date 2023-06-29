package ru.hh.jclient.errors.impl.check;

import jakarta.annotation.Nullable;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.EmptyOrErrorWithStatus;
import ru.hh.jclient.common.EmptyWithStatus;
import ru.hh.jclient.errors.impl.OperationBase;
import ru.hh.jclient.errors.impl.PredicateWithStatus;

public class ApplyEmptyResultOrErrorOperation<E> extends OperationBase<ApplyEmptyResultOrErrorOperation<E>> {

  protected static final Logger logger = LoggerFactory.getLogger(ApplyEmptyResultOrErrorOperation.class);

  protected EmptyOrErrorWithStatus<E> wrapper;
  protected List<PredicateWithStatus<E>> predicates;
  protected boolean returnEmpty;

  public ApplyEmptyResultOrErrorOperation(
      EmptyOrErrorWithStatus<E> wrapper,
      @Nullable Integer errorStatusCode,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<E>> predicates,
      boolean returnEmpty
  ) {
    super(
        AbstractOperation.getStatusCodeIfAbsent(wrapper, errorStatusCode, List.of(), null).orElse(null),
        wrapper.getStatusCode(),
        errorMessage
    );
    this.wrapper = wrapper;
    this.predicates = predicates;
    this.returnEmpty = returnEmpty;
  }

  /**
   * <p>
   * Returns result or throws {@link WebApplicationException} with provided status code on any error including:
   * <ul>
   * <li>{@link EmptyOrErrorWithStatus#isSuccess()} is false</li>
   * <li>predicate provided with {@link AbstractOperationSelector#failIf(java.util.function.Predicate)} says Error contains incorrect value
   * </li>
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
  public EmptyWithStatus onAnyError() {
    if (wrapper.getError().isEmpty()) {
      return new EmptyWithStatus(wrapper.getStatusCode());
    }
    checkForPredicates(wrapper.getError().get()); // set proper status if matched
    return emptyOrThrow("error is not empty");
  }

  protected void checkForPredicates(E error) {
    predicates.forEach(p -> testPredicate(p, error));
  }

  private void testPredicate(PredicateWithStatus<E> predicate, E error) {
    if (predicate.getPredicate().test(error)) {
      this.errorStatusCode = predicate.getStatus().orElse(errorStatusCode);
    }
  }

  protected EmptyWithStatus emptyOrThrow(String cause) {
    if (returnEmpty) {
      logger.warn("Default value is set to result because error happened: {}. Description: {}", cause, exceptionBuilder.getMessage());
      return new EmptyWithStatus(wrapper.getStatusCode());
    }
    throw toException(cause);
  }

  // chaining setter methods

  /**
   * Same as {@link #as(BiFunction)}} but accepts function that knows about Error instance for better error creation.
   */
  public ApplyEmptyResultOrErrorOperation<E> as(Function<E, BiFunction<String, Integer, Object>> errorEntityCreator) {
    return super.as(() -> errorEntityCreator.apply(wrapper.getError().get()));
  }

}
