package ru.hh.jclient.errors.impl.check;

import java.util.function.Consumer;
import java.util.function.Supplier;
import ru.hh.jclient.common.ResultWithStatus;

public class HandleResultOperationSelector<T> extends AbstractOperationSelector<T, HandleResultOperationSelector<T>> {

  private ResultWithStatus<T> resultWithStatus;
  private Throwable throwable;

  public HandleResultOperationSelector(ResultWithStatus<T> resultWithStatus, Throwable throwable, String errorMessage, Object... params) {
    super(errorMessage, params);
    this.resultWithStatus = resultWithStatus;
    this.throwable = throwable;
  }

  /**
   * Specifies that any errors (incorrect result or exception) should be ignored and empty result returned.
   */
  public HandleResultOperation<T> ignore() {
    return new HandleResultOperation<>(resultWithStatus, throwable, errorMessage, predicates, null, null, allowedStatuses, exceptionBuilder);
  }

  /**
   * Specifies default value to return if result is incorrect or exception has occurred.
   *
   * @param defaultValue
   *          default value to return
   */
  public HandleResultOperation<T> returnDefault(T defaultValue) {
    return new HandleResultOperation<>(
        resultWithStatus,
        throwable,
        errorMessage,
        predicates,
        defaultValue,
        null,
        allowedStatuses,
        exceptionBuilder);
  }

  /**
   * Specifies default value to return if result is incorrect or exception has occurred.
   *
   * @param defaultValue
   *          default value to return
   */
  public HandleResultOperation<T> returnDefault(Supplier<T> defaultValue) {
    return new HandleResultOperation<>(
        resultWithStatus,
        throwable,
        errorMessage,
        predicates,
        defaultValue.get(),
        null,
        allowedStatuses,
        exceptionBuilder);
  }

  /**
   * Specifies error consumer to call if exception has occurred. Empty result will be returned in that case.
   *
   * @param consumer
   *          error consumer
   */
  public HandleResultOperation<T> acceptError(Consumer<Throwable> consumer) {
    return new HandleResultOperation<>(
        resultWithStatus,
        throwable,
        errorMessage,
        predicates,
        null,
        consumer,
        allowedStatuses,
        exceptionBuilder);
  }
}
