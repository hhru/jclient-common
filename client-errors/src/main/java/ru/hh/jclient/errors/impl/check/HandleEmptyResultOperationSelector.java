package ru.hh.jclient.errors.impl.check;

import java.util.function.Consumer;
import ru.hh.jclient.common.EmptyWithStatus;

public class HandleEmptyResultOperationSelector extends AbstractOperationSelector<Void, HandleEmptyResultOperationSelector> {

  private EmptyWithStatus emptyWithStatus;
  private Throwable throwable;

  public HandleEmptyResultOperationSelector(EmptyWithStatus emptyWithStatus, Throwable throwable, String errorMessage, Object... params) {
    super(errorMessage, params);
    this.emptyWithStatus = emptyWithStatus;
    this.throwable = throwable;
  }

  /**
   * Specifies that any errors (incorrect result or exception) should be ignored and empty result returned.
   */
  public HandleEmptyResultOperation ignore() {
    return new HandleEmptyResultOperation(emptyWithStatus, throwable, errorMessage, predicates, null, allowedStatuses, exceptionBuilder);
  }

  /**
   * Specifies error consumer to call if exception has occurred. Empty result will be returned in that case.
   *
   * @param consumer
   *          error consumer
   */
  public HandleEmptyResultOperation acceptError(Consumer<Throwable> consumer) {
    return new HandleEmptyResultOperation(emptyWithStatus, throwable, errorMessage, predicates, consumer, allowedStatuses, exceptionBuilder);
  }
}
