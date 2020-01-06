package ru.hh.jclient.errors.impl.check;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.impl.PredicateWithStatus;
import static java.util.Optional.empty;

public class HandleEmptyResultOperation extends AbstractOperation<Void, HandleEmptyResultOperation> {

  private Throwable throwable;
  private Optional<Consumer<Throwable>> errorConsumer;

  public HandleEmptyResultOperation(ResultWithStatus<Void> wrapper,
      Throwable throwable,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<Void>> predicates,
      Optional<Consumer<Throwable>> errorConsumer) {
    super(wrapper, empty(), empty(), empty(), errorMessage, predicates, empty());
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
      logger.warn("Exception happened but was intendedly ignored: {} ({})", throwable.toString(), errorResponseBuilder.getMessage());
      errorConsumer.ifPresent(c -> c.accept(throwable));
      return defaultValue;
    }
    return checkForStatusCodeError();
  }
}
