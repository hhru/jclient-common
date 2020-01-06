package ru.hh.jclient.errors.impl.check;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.impl.PredicateWithStatus;

public class HandleEmptyResultOperationSelector
    extends AbstractHandleResultOperationSelector<Void, HandleEmptyResultOperationSelector, HandleEmptyResultOperation> {


  public HandleEmptyResultOperationSelector(ResultWithStatus<Void> resultWithStatus, Throwable throwable, String errorMessage, Object... params) {
    super(resultWithStatus, throwable, errorMessage, params);
  }

  @Override
  protected HandleEmptyResultOperation createOperation(ResultWithStatus<Void> wrapper,
      Throwable throwable,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<Void>> predicates,
      Optional<Void> defaultValue,
      Optional<Consumer<Throwable>> errorConsumer) {
    return new HandleEmptyResultOperation(wrapper, throwable, errorMessage, predicates, errorConsumer);
  }
}
