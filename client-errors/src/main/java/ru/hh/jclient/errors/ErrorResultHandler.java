package ru.hh.jclient.errors;

import static java.util.Optional.empty;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.ResultOrErrorWithStatus;
import ru.hh.jclient.common.ResultWithStatus;

public class ErrorResultHandler<T, E> extends AbstractErrorHandlerBase<ErrorResultHandler<T, E>> {

  protected static final Logger logger = LoggerFactory.getLogger(ErrorResultHandler.class);

  protected ResultOrErrorWithStatus<T, E> wrapper;

  protected Optional<List<PredicateWithStatus<E>>> predicates = empty();
  protected Optional<T> defaultValue = empty();

  public ErrorResultHandler(ResultOrErrorWithStatus<T, E> wrapper, Optional<Integer> errorStatusCode, String errorMessage, Optional<T> defaultValue) {
    super(errorStatusCode, errorMessage);
    this.wrapper = wrapper;
    this.defaultValue = defaultValue;
    this.errorStatusCode = AbstractErrorHandler.getStatusCodeIfAbsent(wrapper, errorStatusCode, empty(), empty());
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Class<ErrorResultHandler<T, E>> getDerivedClass() {
    return (Class<ErrorResultHandler<T, E>>) getClass();
  }

  public ResultWithStatus<T> onAnyError() {
    if (!wrapper.getError().isPresent()) {
      return wrapper;
    }
    checkForPredicates(wrapper.getError().get()); // set proper status if matched
    return defaultOrThrow("error is not empty");
  }

  protected void checkForPredicates(E error) {
    predicates.ifPresent(pp -> pp.forEach(p -> testPredicate(p, error)));
  }

  private void testPredicate(PredicateWithStatus<E> predicate, E error) {
    if (predicate.getPredicate().test(error)) {
      this.errorStatusCode = predicate.getStatus().map(Optional::of).orElse(this.errorStatusCode);
    }
  }

  protected ResultWithStatus<T> defaultOrThrow(String cause) {
    if (defaultValue.isPresent()) {
      logger.warn("Default value is set to result because error happened: {}. Description: {}", cause, errorResponseBuilder.getMessage());
      return new ResultWithStatus<>(defaultValue.get(), wrapper.getStatusCode());
    }
    throw toException(cause);
  }

  // chaining setter methods

  /**
   * Sets predicates to be checked against returned result.
   */
  protected ErrorResultHandler<T, E> alsoFailOn(List<PredicateWithStatus<E>> predicates) {
    this.predicates = Optional.ofNullable(predicates);
    return this;
  }

  /**
   * Same as {@link #as(BiFunction)}} but accepts function that knows about Error instance for better error creation.
   */
  public ErrorResultHandler<T, E> as(Function<E, BiFunction<String, Integer, Object>> errorEntityCreator) {
    return super.as(() -> errorEntityCreator.apply(wrapper.getError().get()));
  }

}
