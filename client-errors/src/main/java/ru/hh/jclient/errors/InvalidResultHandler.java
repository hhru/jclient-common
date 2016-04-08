package ru.hh.jclient.errors;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.ws.rs.WebApplicationException;
import ru.hh.jclient.common.ResultWithStatus;

/**
 * Contains useful methods to handle error outcome of {@link CompletableFuture} with different jclient-common wrappers.
 */
public class InvalidResultHandler<T> extends AbstractErrorHandler<T, InvalidResultHandler<T>> {

  // constructors

  InvalidResultHandler(
      ResultWithStatus<T> wrapper,
      Optional<Integer> errorStatusCode,
      Optional<List<Integer>> proxiedStatusCodes,
      Optional<Function<Integer, Integer>> statusCodesConverter,
      String errorMessage) {
    super(wrapper, errorStatusCode, proxiedStatusCodes, statusCodesConverter, errorMessage);
  }

  InvalidResultHandler(
      ResultWithStatus<T> wrapper,
      Optional<Integer> errorStatusCode,
      Optional<List<Integer>> proxiedStatusCodes,
      Optional<Function<Integer, Integer>> statusCodesConverter,
      String errorMessage,
      Optional<T> defaultValue) {
    super(wrapper, errorStatusCode, proxiedStatusCodes, statusCodesConverter, errorMessage, defaultValue);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Class<InvalidResultHandler<T>> getDerivedClass() {
    return (Class<InvalidResultHandler<T>>) getClass();
  }

  // terminal operations

  /**
   * Returns result or throws {@link WebApplicationException} with provided status code on any error including:
   * <ul>
   * <li>{@link ResultWithStatus#isSuccess()} is false</li>
   * <li>predicate provided with {@link #alsoFailOn(java.util.function.Predicate)} says ResultWithStatus contains incorrect value</li>
   * <li>{@link ResultWithStatus#get()} contains {@link Optional#empty()}</li>
   * </ul>
   *
   * If default value is specified, it will be returned instead of exception.
   *
   * @throws WebApplicationException
   *           with provided status code and message in case of error (if default value is not specified)
   * @return unwrapped non null result or default value (if specified) in case of error
   */
  public T onAnyError() {
    return checkForAnyError().get();
  }

  /**
   * Returns result or throws {@link WebApplicationException} with provided status code if {@link ResultWithStatus#get()} contains
   * {@link Optional#empty()}.
   *
   * If default value is specified, it will be returned instead of exception.
   *
   * @throws WebApplicationException
   *           with provided status code and message in case of error (if default value is not specified)
   * @return unwrapped non null result or default value (if specified) in case of error
   */
  public T onEmpty() {
    return checkForEmpty();
  }

  /**
   * Returns result or throws {@link WebApplicationException} with provided status code on status code error - if {@link ResultWithStatus#isSuccess()}
   * is false.
   *
   * If default value is specified, it will be returned instead of exception.
   *
   * @throws WebApplicationException
   *           with provided status code and message in case of error (if default value is not specified)
   * @return result or default value (if specified) in case of error
   */
  public Optional<T> onStatusCodeError() {
    return checkForStatusCodeError();
  }

  /**
   * Returns result or throws {@link WebApplicationException} with provided status code if predicate fails.
   *
   * If default value is specified, it will be returned instead of exception.
   *
   * @throws WebApplicationException
   *           with provided status code and message in case of error (if default value is not specified)
   * @return result or default value (if specified) in case of error
   */
  public Optional<T> onPredicate() {
    return checkForPredicates(wrapper.get());
  }

}
