package ru.hh.jclient.errors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import ru.hh.jclient.common.EmptyOrErrorWithStatus;
import ru.hh.jclient.common.EmptyWithStatus;
import ru.hh.jclient.common.ResultOrErrorWithStatus;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.impl.check.ApplyEmptyResultOperationSelector;
import ru.hh.jclient.errors.impl.check.ApplyEmptyResultOrErrorOperationSelector;
import ru.hh.jclient.errors.impl.check.ApplyResultOperationSelector;
import ru.hh.jclient.errors.impl.check.ApplyResultOrErrorOperationSelector;
import ru.hh.jclient.errors.impl.check.HandleEmptyResultOperationSelector;
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
   * Checks result / throwable for errors. Compatible with methods of {@link CompletableFuture} like
   * {@link CompletableFuture#handle(java.util.function.BiFunction)}.
   *
   * @param emptyWithStatus
   *          result to check
   * @param throwable
   *          throwable to check
   * @param errorMessage
   *          message to include in exception / log if error is detected
   * @param errorMessageParams
   *          if specified, used to format errorMessage using {@link String#format(String, Object...)}
   */
  public static HandleEmptyResultOperationSelector check(
      EmptyWithStatus emptyWithStatus,
      Throwable throwable,
      String errorMessage,
      Object... errorMessageParams) {
    return new HandleEmptyResultOperationSelector(emptyWithStatus, throwable, errorMessage, errorMessageParams);
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
   * Checks result error if present. Compatible with methods of {@link CompletableFuture} like
   * {@link CompletableFuture#thenApply(java.util.function.Function)}.
   *
   * @param emptyOrErrorWithStatus
   *          result to check
   * @param errorMessage
   *          message to include in exception / log if error is detected
   * @param errorMessageParams
   *          if specified, used to format errorMessage using {@link String#format(String, Object...)}
   */
  public static <E> ApplyEmptyResultOrErrorOperationSelector<E> checkError(
      EmptyOrErrorWithStatus<E> emptyOrErrorWithStatus,
      String errorMessage,
      Object... errorMessageParams) {
    return new ApplyEmptyResultOrErrorOperationSelector<>(emptyOrErrorWithStatus, errorMessage, errorMessageParams);
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

  /**
   * Block until future completes and get the result.
   *
   * If waiting for result was interrupted, set the interrupted flag and rethrow it wrapped in {@link RuntimeException}.
   * If future completed exceptionally with {@link RuntimeException} - rethrow it.
   * If future completed with checked exception, wrap it in {@link RuntimeException} and rethrow it too.
   *
   * @param future future to get the result from
   * @return result or throw RuntimeException
   */
  public static <T> T getOrThrow(CompletableFuture<T> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while waiting for completable future to complete", e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      throw new RuntimeException("Completable future completed exceptionally", cause);
    }
  }

  /**
   * Waited block until future completes and get the result.
   *
   * If waiting for result was interrupted, set the interrupted flag and rethrow it wrapped in {@link RuntimeException}.
   * If future completed exceptionally with {@link RuntimeException} - rethrow it.
   * If future completed with checked exception, wrap it in {@link RuntimeException} and rethrow it too.
   *
   * @param future future to get the result from
   * @return result or throw RuntimeException
   */
  public static <T> T waitedGetOrThrow(CompletableFuture<T> future, long timeout, TimeUnit unit) {
    try {
      return future.get(timeout, unit);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while waiting for completable future to complete", e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      throw new RuntimeException("Completable future completed exceptionally", cause);
    } catch (TimeoutException e) {
      throw new RuntimeException("Failed to wait " + timeout + " " + unit + " for the future", e);
    }
  }

  /**
   * Rethrow RuntimeException or Error, wrap anything other with RuntimeException.
   */
  public static RuntimeException propagate(Throwable throwable) {
    if (throwable instanceof RuntimeException) {
      throw (RuntimeException) throwable;
    }
    if (throwable instanceof Error) {
      throw (Error) throwable;
    }
    throw new RuntimeException(throwable);
  }
}
