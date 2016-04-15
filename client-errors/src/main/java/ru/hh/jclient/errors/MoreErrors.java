package ru.hh.jclient.errors;

import java.util.concurrent.CompletableFuture;
import ru.hh.jclient.common.ResultOrErrorWithStatus;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.impl.check.ApplyResultOperationSelector;
import ru.hh.jclient.errors.impl.check.ApplyResultOrErrorOperationSelector;
import ru.hh.jclient.errors.impl.check.HandleResultOperationSelector;
import ru.hh.jclient.errors.impl.convert.HandleThrowableOperationSelector;

public class MoreErrors {

  /**
   * Checks result for errors. Compatible with methods of {@link CompletableFuture} like
   * {@link CompletableFuture#thenApply(java.util.function.Function)}.
   *
   * @param resultWithStatus
   *          result to check
   * @param errorMessage
   *          message to include in exception / log if error is detected
   */
  public static <T> ApplyResultOperationSelector<T> check(ResultWithStatus<T> resultWithStatus, String errorMessage) {
    return new ApplyResultOperationSelector<>(resultWithStatus, errorMessage);
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
  public static <T> HandleResultOperationSelector<T> check(ResultWithStatus<T> resultWithStatus, Throwable throwable, String errorMessage) {
    return new HandleResultOperationSelector<>(resultWithStatus, throwable, errorMessage);
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
  public static <T, E> ApplyResultOrErrorOperationSelector<T, E> checkError(ResultOrErrorWithStatus<T, E> resultOrErrorWithStatus, String errorMessage) {
    return new ApplyResultOrErrorOperationSelector<>(resultOrErrorWithStatus, errorMessage);
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
  public static <T> HandleThrowableOperationSelector<T> convertException(T result, Throwable throwable, String errorMessage) {
    return new HandleThrowableOperationSelector<>(result, throwable, errorMessage);
  }
}
