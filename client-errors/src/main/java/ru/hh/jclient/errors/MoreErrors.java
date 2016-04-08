package ru.hh.jclient.errors;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import ru.hh.errors.common.Errors;
import ru.hh.jclient.common.ResultOrErrorWithStatus;
import ru.hh.jclient.common.ResultWithStatus;

public class MoreErrors {

  static final int BAD_GATEWAY = 502;
  static final int GATEWAY_TIMEOUT = 504;

  /**
   * Checks result for errors. Compatible with methods of {@link CompletableFuture} like
   * {@link CompletableFuture#thenApply(java.util.function.Function)}.
   *
   * @param resultWithStatus
   *          result to check
   * @param errorMessage
   *          message to include in exception / log if error is detected
   */
  public static <T> ResultWithStatusChecker<T> check(ResultWithStatus<T> resultWithStatus, String errorMessage) {
    return new ResultWithStatusChecker<>(resultWithStatus, errorMessage);
  }

  /**
   * Checks result / throwable for errors. Compatible with methods of {@link CompletableFuture} like
   * {@link CompletableFuture#handle(java.util.function.BiFunction)}.
   *
   * @param resultWithStatus
   *          result to check
   * @param throwable
   *          throwable to check
   * @param errorMessage
   *          message to include in exception / log if error is detected
   */
  public static <T> ResultWithStatusOrThrowableChecker<T> check(ResultWithStatus<T> resultWithStatus, Throwable throwable, String errorMessage) {
    return new ResultWithStatusOrThrowableChecker<>(resultWithStatus, throwable, errorMessage);
  }

  /**
   * Converts exception if available. Compatible with methods of {@link CompletableFuture} like
   * {@link CompletableFuture#handle(java.util.function.BiFunction)}.
   *
   * @param result
   *          result to return if no exception
   * @param throwable
   *          throwable to convert
   * @param errorMessage
   *          message to include in exception / log if error is detected
   */
  public static <T> ResultOrThrowableChecker<T> convert(T result, Throwable throwable, String errorMessage) {
    return new ResultOrThrowableChecker<>(result, throwable, errorMessage);
  }

  /**
   * Checks result error if present. Compatible with methods of {@link CompletableFuture} like
   * {@link CompletableFuture#thenApply(java.util.function.Function)}.
   *
   * @param resultOrErrorWithStatus
   *          result to check
   * @param errorMessage
   *          message to include in exception / log if error is detected
   */
  public static <T, E> ResultOrErrorChecker<T, E> checkError(ResultOrErrorWithStatus<T, E> resultOrErrorWithStatus, String errorMessage) {
    return new ResultOrErrorChecker<>(resultOrErrorWithStatus, errorMessage);
  }

  /**
   * Returns function to use with {@link AbstractErrorHandlerBase#as(BiFunction)}. Function will produce {@link Errors} object with specified error
   * key.
   *
   * @param key
   *          error key
   */
  public static BiFunction<String, Integer, Object> error(Object key) {
    return (s, i) -> Errors.of(i, key.toString(), s);
  }

  /**
   * Returns {@link WebApplicationException} with {@link Errors} as response body.
   *
   * @param code
   *          status code to set to error response
   * @param errorKey
   *          error key
   * @param description
   *          error description
   */
  public static WebApplicationException error(int code, Object errorKey, String description, Throwable cause) {
    return new ErrorResponseBuilder(description).setStatus(code).setEntityCreator(error(errorKey)).setCause(cause).toException();
  }

  /**
   * Returns {@link WebApplicationException} with provided error container as response body.
   *
   * @param errors
   *          errors container
   */
  public static WebApplicationException error(Errors errors) {
    return new ErrorResponseBuilder(null).setStatus(errors.code).setEntityCreator((s, i) -> errors).toException();
  }

  /**
   * Returns {@link WebApplicationException} with {@link Errors} as response body.
   *
   * @param status
   *          status code to set to error response
   * @param errorKey
   *          error key
   * @param description
   *          error description
   */
  public static WebApplicationException error(Status status, Object errorKey, String description, Throwable cause) {
    return error(status.getStatusCode(), errorKey, description, cause);
  }

  public static WebApplicationException errorBadRequest(Object errorKey, String description) {
    return error(BAD_REQUEST, errorKey, description, null);
  }

  public static <T> T throwBadRequest(Object errorKey, String description) {
    throw errorBadRequest(errorKey, description);
  }

  public static WebApplicationException errorConflict(Object errorKey, String description) {
    return error(CONFLICT, errorKey, description, null);
  }

  public static <T> T throwConflict(Object errorKey, String description) {
    throw errorConflict(errorKey, description);
  }

  public static WebApplicationException errorNotFound(Object errorKey, String description) {
    return error(NOT_FOUND, errorKey, description, null);
  }

  public static <T> T throwNotFound(Object errorKey, String description) {
    throw errorNotFound(errorKey, description);
  }

  public static WebApplicationException errorForbidden(Object errorKey, String description) {
    return error(FORBIDDEN, errorKey, description, null);
  }

  public static <T> T throwForbidden(Object errorKey, String description) {
    throw errorForbidden(errorKey, description);
  }

  private static abstract class CheckerWitMessage {

    protected String errorMessage;

    public CheckerWitMessage(String errorMessage) {
      this.errorMessage = errorMessage;
    }

  }

  private static abstract class CheckerWithPredicates<T, D extends CheckerWithPredicates<T, D>> extends CheckerWitMessage {

    public CheckerWithPredicates(String errorMessage) {
      super(errorMessage);
    }

    protected List<PredicateWithStatus<T>> predicates = null;

    private List<PredicateWithStatus<T>> predicates() {
      if (predicates == null) {
        predicates = new ArrayList<>();
      }
      return predicates;
    }

    /**
     * Specifies predicate that will be checked against result. Failed predicate means result is INCORRECT. If called multiple times, resulting
     * predicate will be built with {@link Predicate#or(Predicate)}.
     *
     * @param predicate
     *          predicate to add
     */
    public D alsoFailIf(Predicate<T> predicate) {
      return alsoFailIf(predicate, (Integer) null);
    }

    /**
     * Specifies predicate that will be checked against result. Failed predicate means result is INCORRECT. If called multiple times, resulting
     * predicate will be built with {@link Predicate#or(Predicate)}.
     *
     * @param predicate
     *          predicate to add
     * @param status
     *          response status code to set in case predicate matched
     *
     */
    public D alsoFailIf(Predicate<T> predicate, Integer status) {
      predicates().add(new PredicateWithStatus<>(predicate, status));
      return getDerivedClass().cast(this);
    }

    /**
     * Specifies predicate that will be checked against result. Failed predicate means result is INCORRECT. If called multiple times, resulting
     * predicate will be built with {@link Predicate#or(Predicate)}.
     *
     * @param predicate
     *          predicate to add
     * @param status
     *          response status code to set in case predicate matched
     *
     */
    public D alsoFailIf(Predicate<T> predicate, Status status) {
      return alsoFailIf(predicate, status.getStatusCode());
    }

    protected abstract Class<D> getDerivedClass();
  }

  // invalid result checker

  public abstract static class ThrowingResultWithStatusChecker<T, Z extends ThrowingResultWithStatusChecker<T, Z>>
      extends CheckerWithPredicates<T, Z> {

    protected ResultWithStatus<T> resultWithStatus;
    private List<Integer> proxiedStatusCodes;
    private Function<Integer, Integer> statusCodesConverter;

    private ThrowingResultWithStatusChecker(ResultWithStatus<T> resultWithStatus, String errorMessage) {
      super(errorMessage);
      this.resultWithStatus = resultWithStatus;
    }

    /**
     * Specifies what statuses to proxy. All other statuses will be converted to specified THROW_* status. Calling this method will override previous
     * value.
     */
    public ThrowingResultWithStatusChecker<T, Z> proxyOnly(Integer... statuses) {
      proxiedStatusCodes = Arrays.asList(statuses);
      return this;
    }

    /**
     * Specifies what statuses to proxy. All other statuses will be converted to specified THROW_* status.
     */
    public ThrowingResultWithStatusChecker<T, Z> proxyOnly(Status... statuses) {
      proxiedStatusCodes = Stream.of(statuses).map(Status::getStatusCode).collect(toList());
      return this;
    }

    /**
     * Specifies how matching response status code must be converted. Converting only happens for error cases. If status code is converted it will be
     * proxied (overriding THROW_* status code). This method can be called multiple times and will chain conversion in the corresponding order.
     */
    public ThrowingResultWithStatusChecker<T, Z> convertAndProxy(Integer source, Integer target) {
      Function<Integer, Integer> converter = i -> i.equals(source) ? target : i;
      if (statusCodesConverter == null) {
        statusCodesConverter = converter;
      }
      else {
        statusCodesConverter = statusCodesConverter.andThen(converter);
      }
      return this;
    }

    /**
     * Specifies how response status code must be converted if matched. Converting only happens for error cases. This method can be called multiple
     * times and will chain conversion.
     */
    public ThrowingResultWithStatusChecker<T, Z> convertAndProxy(Status source, Status target) {
      return convertAndProxy(source.getStatusCode(), target.getStatusCode());
    }

    /**
     * Uses {@link Status#INTERNAL_SERVER_ERROR} status code.
     */
    public InvalidResultHandler<T> THROW_INTERNAL_SERVER_ERROR() {
      return new InvalidResultHandler<>(
          resultWithStatus,
          of(INTERNAL_SERVER_ERROR.getStatusCode()),
          ofNullable(proxiedStatusCodes),
          ofNullable(statusCodesConverter),
          errorMessage).alsoFailOn(predicates);
    }

    /**
     * Uses {@link Status#BAD_GATEWAY} status code.
     */
    public InvalidResultHandler<T> THROW_BAD_GATEWAY() {
      return new InvalidResultHandler<>(
          resultWithStatus,
          of(BAD_GATEWAY),
          ofNullable(proxiedStatusCodes),
          ofNullable(statusCodesConverter),
          errorMessage).alsoFailOn(predicates);
    }

    /**
     * Uses {@link Status#FORBIDDEN} status code.
     */
    public InvalidResultHandler<T> THROW_FORBIDDEN() {
      return new InvalidResultHandler<>(
          resultWithStatus,
          of(FORBIDDEN.getStatusCode()),
          ofNullable(proxiedStatusCodes),
          ofNullable(statusCodesConverter),
          errorMessage).alsoFailOn(predicates);
    }

  }

  public static class ResultWithStatusChecker<T> extends ThrowingResultWithStatusChecker<T, ResultWithStatusChecker<T>> {

    private ResultWithStatusChecker(ResultWithStatus<T> resultWithStatus, String errorMessage) {
      super(resultWithStatus, errorMessage);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Class<ResultWithStatusChecker<T>> getDerivedClass() {
      return (Class<ResultWithStatusChecker<T>>) getClass();
    }

    /**
     * Uses status code from {@link ResultWithStatus#getStatusCode()} (response code from remote call). Note that {@link Status#SERVICE_UNAVAILABLE}
     * will be replaced with {@link Status#BAD_GATEWAY}.
     */
    public InvalidResultHandler<T> PROXY_STATUS_CODE() {
      return new InvalidResultHandler<>(resultWithStatus, empty(), empty(), empty(), errorMessage).alsoFailOn(predicates);
    }

    /**
     * Sets default value to return.
     *
     * @param value
     *          default value to return in case of error
     */
    public InvalidResultHandler<T> RETURN_DEFAULT(T value) {
      return new InvalidResultHandler<>(resultWithStatus, empty(), empty(), empty(), errorMessage, of(value)).alsoFailOn(predicates);
    }

    /**
     * Sets default value to return.
     *
     * @param value
     *          default value to return in case of error
     */
    public InvalidResultHandler<T> RETURN_DEFAULT(Supplier<T> value) {
      return new InvalidResultHandler<>(resultWithStatus, empty(), empty(), empty(), errorMessage, of(value.get())).alsoFailOn(predicates);
    }
  }

  // ignore errors checker

  public static class ResultWithStatusOrThrowableChecker<T> extends CheckerWithPredicates<T, ResultWithStatusOrThrowableChecker<T>> {

    private ResultWithStatus<T> resultWithStatus;
    private Throwable throwable;

    private ResultWithStatusOrThrowableChecker(ResultWithStatus<T> resultWithStatus, Throwable throwable, String errorMessage) {
      super(errorMessage);
      this.resultWithStatus = resultWithStatus;
      this.throwable = throwable;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Class<ResultWithStatusOrThrowableChecker<T>> getDerivedClass() {
      return (Class<ResultWithStatusOrThrowableChecker<T>>) getClass();
    }

    /**
     * Specifies that any errors (incorrect result or exception) should be ignored and empty result returned.
     */
    public IgnoreErrorHandler<T> IGNORE() {
      return new IgnoreErrorHandler<>(resultWithStatus, throwable, errorMessage, empty(), empty()).alsoFailOn(predicates);
    }

    /**
     * Specifies default value to return if result is incorrect or exception has occurred.
     *
     * @param defaultValue
     *          default value to return
     */
    public IgnoreErrorHandler<T> RETURN_DEFAULT(T defaultValue) {
      return new IgnoreErrorHandler<>(resultWithStatus, throwable, errorMessage, of(defaultValue), empty()).alsoFailOn(predicates);
    }

    /**
     * Specifies default value to return if result is incorrect or exception has occurred.
     *
     * @param defaultValue
     *          default value to return
     */
    public IgnoreErrorHandler<T> RETURN_DEFAULT(Supplier<T> defaultValue) {
      return new IgnoreErrorHandler<>(resultWithStatus, throwable, errorMessage, of(defaultValue.get()), empty()).alsoFailOn(predicates);
    }

    /**
     * Specifies error consumer to call if exception has occurred. Empty result will be returned in that case.
     *
     * @param consumer
     *          error consumer
     */
    public IgnoreErrorHandler<T> ACCEPT_ERROR(Consumer<Throwable> consumer) {
      return new IgnoreErrorHandler<>(resultWithStatus, throwable, errorMessage, empty(), of(consumer)).alsoFailOn(predicates);
    }

  }

  // convert errors checker

  public static class ResultOrThrowableChecker<T> extends CheckerWitMessage {

    private T result;
    private Throwable throwable;

    private ResultOrThrowableChecker(T result, Throwable throwable, String errorMessage) {
      super(errorMessage);
      this.result = result;
      this.throwable = throwable;
    }

    public ClientExceptionHandler<T> THROW_BAD_GATEWAY() {
      return new ClientExceptionHandler<>(result, throwable, BAD_GATEWAY, errorMessage);
    }

    public ClientExceptionHandler<T> THROW_GATEWAY_TIMEOUT() {
      return new ClientExceptionHandler<>(result, throwable, GATEWAY_TIMEOUT, errorMessage);
    }

    public ClientExceptionHandler<T> THROW_INTERNAL_SERVER_ERROR() {
      return new ClientExceptionHandler<>(result, throwable, INTERNAL_SERVER_ERROR.getStatusCode(), errorMessage);
    }
  }

  // ResultOrErrorWithStatus checker

  public static class ResultOrErrorChecker<T, E> extends CheckerWithPredicates<E, ResultOrErrorChecker<T, E>> {

    private ResultOrErrorWithStatus<T, E> resultOrErrorWithStatus;

    public ResultOrErrorChecker(ResultOrErrorWithStatus<T, E> resultOrErrorWithStatus, String errorMessage) {
      super(errorMessage);
      this.resultOrErrorWithStatus = resultOrErrorWithStatus;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Class<ResultOrErrorChecker<T, E>> getDerivedClass() {
      return (Class<ResultOrErrorChecker<T, E>>) getClass();
    }

    /**
     * Specifies default value to set to result if error is present.
     *
     * @param defaultValue
     *          default value to set
     */
    public ErrorResultHandler<T, E> SET_DEFAULT(T defaultValue) {
      return new ErrorResultHandler<>(resultOrErrorWithStatus, empty(), errorMessage, of(defaultValue)).alsoFailOn(predicates);
    }

    /**
     * Specifies default value to set to result if error is present.
     *
     * @param defaultValue
     *          default value to set
     */
    public ErrorResultHandler<T, E> SET_DEFAULT(Supplier<T> defaultValue) {
      return new ErrorResultHandler<>(resultOrErrorWithStatus, empty(), errorMessage, of(defaultValue.get())).alsoFailOn(predicates);
    }

    public ErrorResultHandler<T, E> THROW_BAD_GATEWAY() {
      return new ErrorResultHandler<>(resultOrErrorWithStatus, of(BAD_GATEWAY), errorMessage, empty()).alsoFailOn(predicates);
    }

    public ErrorResultHandler<T, E> THROW_GATEWAY_TIMEOUT() {
      return new ErrorResultHandler<>(resultOrErrorWithStatus, of(GATEWAY_TIMEOUT), errorMessage, empty())
          .alsoFailOn(predicates);
    }

    public ErrorResultHandler<T, E> THROW_INTERNAL_SERVER_ERROR() {
      return new ErrorResultHandler<>(resultOrErrorWithStatus, of(INTERNAL_SERVER_ERROR.getStatusCode()), errorMessage, empty())
          .alsoFailOn(predicates);
    }

    public ErrorResultHandler<T, E> THROW_CONFLICT() {
      return new ErrorResultHandler<>(resultOrErrorWithStatus, of(CONFLICT.getStatusCode()), errorMessage, empty()).alsoFailOn(predicates);
    }

    public ErrorResultHandler<T, E> THROW_FORBIDDEN() {
      return new ErrorResultHandler<>(resultOrErrorWithStatus, of(FORBIDDEN.getStatusCode()), errorMessage, empty()).alsoFailOn(predicates);
    }

    /**
     * Uses status code from {@link ResultOrErrorWithStatus#getStatusCode()} (response code from remote call). Note that
     * {@link Status#SERVICE_UNAVAILABLE} will be replaced with {@link Status#BAD_GATEWAY}.
     */
    public ErrorResultHandler<T, E> PROXY_STATUS_CODE() {
      return new ErrorResultHandler<>(resultOrErrorWithStatus, empty(), errorMessage, empty()).alsoFailOn(predicates);
    }

  }

}
