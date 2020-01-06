package ru.hh.jclient.errors;

import java.util.concurrent.CompletableFuture;
import ru.hh.jclient.common.EmptyWithStatus;
import ru.hh.jclient.common.ResultOrErrorWithStatus;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.impl.check.ApplyEmptyResultOperationSelector;
import ru.hh.jclient.errors.impl.check.ApplyResultOperationSelector;
import ru.hh.jclient.errors.impl.check.ApplyResultOrErrorOperationSelector;
import ru.hh.jclient.errors.impl.check.HandleResultOperationSelector;
import ru.hh.jclient.errors.impl.convert.HandleThrowableOperationSelector;

public class MoreErrors {

  private MoreErrors() {
  }

  /**
   * Checks result for errors. Compatible with methods of {@link CompletableFuture} like
   * {@link CompletableFuture#thenApply(java.util.function.Function)}.
   *
   * @param resultWithStatus
   *          result to check
   * @param errorMessage
   *          message to include in exception / log if error is detected
   * @param errorMessageParams
   *          if specified, used to format errorMessage using {@link String#format(String, Object...)}
   */
  public static <T> ApplyResultOperationSelector<T> check(ResultWithStatus<T> resultWithStatus, String errorMessage, Object... errorMessageParams) {
    return new ApplyResultOperationSelector<>(resultWithStatus, errorMessage, errorMessageParams);
  }

  /**
   * Checks result for errors. Compatible with methods of {@link CompletableFuture} like
   * {@link CompletableFuture#thenApply(java.util.function.Function)}.
   *
   * @param emptyWithStatus
   *          empty result (i.e. response of 204 NO_CONTENT) to check
   * @param errorMessage
   *          message to include in exception / log if error is detected
   * @param errorMessageParams
   *          if specified, used to format errorMessage using {@link String#format(String, Object...)}
   */
  public static ApplyEmptyResultOperationSelector check(EmptyWithStatus emptyWithStatus, String errorMessage, Object... errorMessageParams) {
    return new ApplyEmptyResultOperationSelector(emptyWithStatus, errorMessage, errorMessageParams);
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
   * @param errorMessageParams
   *          if specified, used to format errorMessage using {@link String#format(String, Object...)}
   */
  public static <T> HandleResultOperationSelector<T> check(
      ResultWithStatus<T> resultWithStatus,
      Throwable throwable,
      String errorMessage,
      Object... errorMessageParams) {
    return new HandleResultOperationSelector<>(resultWithStatus, throwable, errorMessage, errorMessageParams);
  }

  /**
   * Checks result error if present. Compatible with methods of {@link CompletableFuture} like
   * {@link CompletableFuture#thenApply(java.util.function.Function)}.
   *
   * @param resultOrErrorWithStatus
   *          result to check
   * @param errorMessage
   *          message to include in exception / log if error is detected
   * @param errorMessageParams
   *          if specified, used to format errorMessage using {@link String#format(String, Object...)}
   */
  public static <T, E> ApplyResultOrErrorOperationSelector<T, E> checkError(
      ResultOrErrorWithStatus<T, E> resultOrErrorWithStatus,
      String errorMessage,
      Object... errorMessageParams) {
    return new ApplyResultOrErrorOperationSelector<>(resultOrErrorWithStatus, errorMessage, errorMessageParams);
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
   * @param errorMessageParams
   *          if specified, used to format errorMessage using {@link String#format(String, Object...)}
   */
  public static <T> HandleThrowableOperationSelector<T> convertException(
      T result,
      Throwable throwable,
      String errorMessage,
      Object... errorMessageParams) {
    return new HandleThrowableOperationSelector<>(result, throwable, errorMessage, errorMessageParams);
  }
}
