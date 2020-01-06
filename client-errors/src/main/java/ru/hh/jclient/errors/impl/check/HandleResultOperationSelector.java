package ru.hh.jclient.errors.impl.check;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.impl.PredicateWithStatus;
import static java.util.Optional.empty;
import static java.util.Optional.of;

public class HandleResultOperationSelector<T> extends AbstractHandleResultOperationSelector<T, HandleResultOperationSelector<T>, HandleResultOperation<T>> {

  public HandleResultOperationSelector(ResultWithStatus<T> resultWithStatus, Throwable throwable, String errorMessage, Object... params) {
    super(resultWithStatus, throwable, errorMessage, params);
  }

  @Override
  protected HandleResultOperation<T> createOperation(ResultWithStatus<T> wrapper,
      Throwable throwable,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<T>> predicates,
      Optional<T> defaultValue,
      Optional<Consumer<Throwable>> errorConsumer) {
    return new HandleResultOperation<>(wrapper, throwable, errorMessage, predicates, defaultValue, errorConsumer);
  }

  /**
   * Specifies default value to return if result is incorrect or exception has occurred.
   *
   * @param defaultValue
   *          default value to return
   */
  public HandleResultOperation<T> returnDefault(T defaultValue) {
    return createOperation(resultWithStatus, throwable, errorMessage, predicates, of(defaultValue), empty());
  }

  /**
   * Specifies default value to return if result is incorrect or exception has occurred.
   *
   * @param defaultValue
   *          default value to return
   */
  public HandleResultOperation<T> returnDefault(Supplier<T> defaultValue) {
    return createOperation(resultWithStatus, throwable, errorMessage, predicates, of(defaultValue.get()), empty());
  }

}
