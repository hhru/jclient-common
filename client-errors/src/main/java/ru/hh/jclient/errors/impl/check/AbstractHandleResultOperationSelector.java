package ru.hh.jclient.errors.impl.check;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.impl.PredicateWithStatus;

public abstract class AbstractHandleResultOperationSelector<T,
    D extends AbstractHandleResultOperationSelector<T, D, AO>,
    AO extends AbstractOperation<T, AO>>
    extends AbstractOperationSelector<T, D> {

  protected ResultWithStatus<T> resultWithStatus;
  protected Throwable throwable;

  public AbstractHandleResultOperationSelector(ResultWithStatus<T> resultWithStatus, Throwable throwable, String errorMessage, Object... params) {
    super(errorMessage, params);
    this.resultWithStatus = resultWithStatus;
    this.throwable = throwable;
  }

  /**
   * Specifies that any errors (incorrect result or exception) should be ignored and empty result returned.
   */
  public AO ignore() {
    return createOperation(resultWithStatus, throwable, errorMessage, predicates, empty(), empty());
  }

  /**
   * Specifies error consumer to call if exception has occurred. Empty result will be returned in that case.
   *
   * @param consumer
   *          error consumer
   */
  public AO acceptError(Consumer<Throwable> consumer) {
    return createOperation(resultWithStatus, throwable, errorMessage, predicates, empty(), of(consumer));
  }

  protected abstract AO createOperation(ResultWithStatus<T> wrapper,
      Throwable throwable,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<T>> predicates,
      Optional<T> defaultValue,
      Optional<Consumer<Throwable>> errorConsumer);
}
