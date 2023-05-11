package ru.hh.jclient.errors.impl.check;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.impl.ExceptionBuilder;
import ru.hh.jclient.errors.impl.PredicateWithStatus;

public class HandleEmptyResultOperation extends AbstractOperation<Void, HandleEmptyResultOperation> {

  private Throwable throwable;
  @Nullable
  private Consumer<Throwable> errorConsumer;

  public HandleEmptyResultOperation(
      ResultWithStatus<Void> wrapper,
      Throwable throwable,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<Void>> predicates,
      Consumer<Throwable> errorConsumer,
      Set<Integer> allowedStatuses,
      ExceptionBuilder<?, ?> exceptionBuilder) {
    super(wrapper, null, List.of(), null, errorMessage, predicates, allowedStatuses, exceptionBuilder);
    this.throwable = throwable;
    this.errorConsumer = errorConsumer;
  }

  @Override
  protected boolean useDefault() {
    return true;
  }

  /**
   * Returns Optional with default value (if specified) or empty if:
   *
   * <ul>
   * <li>exception is not null</li>
   * <li>result is incorrect according to {@link ApplyResultOperation#onStatusCodeError()}</li>
   * </ul>
   *
   * otherwise returns Optional with unwrapped result.
   *
   * @return
   */
  public Optional<Void> onStatusCodeError() {
    if (throwable != null) {
      logger.warn("Exception happened but was intendedly ignored: {} ({})", throwable.toString(), exceptionBuilder.getMessage());
      if (errorConsumer != null) {
        errorConsumer.accept(throwable);
      }
      return Optional.ofNullable(defaultValue);
    }
    return checkForStatusCodeError();
  }
}
