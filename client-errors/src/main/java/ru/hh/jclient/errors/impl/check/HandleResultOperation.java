package ru.hh.jclient.errors.impl.check;

import static java.util.Optional.empty;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.impl.PredicateWithStatus;

/**
 * Contains useful methods to handle error outcome of {@link CompletableFuture} with different jclient-common wrappers.
 */
public class HandleResultOperation<T> extends AbstractOperation<T, HandleResultOperation<T>> {

  private Throwable throwable;
  private Optional<Consumer<Throwable>> errorConsumer;

  public HandleResultOperation(
      ResultWithStatus<T> wrapper,
      Throwable throwable,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<T>> predicates,
      Optional<T> defaultValue,
      Optional<Consumer<Throwable>> errorConsumer) {
    super(wrapper, empty(), empty(), empty(), errorMessage, predicates, defaultValue);
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
   * <li>result is incorrect according to {@link ApplyResultOperation#onAnyError()}</li>
   * </ul>
   *
   * otherwise returns Optional with unwrapped result.
   *
   * @return
   */
  public Optional<T> onAnyError() {
    if (throwable != null) {
      logger.warn("Exception happened but was intendedly ignored: {} ({})", throwable.toString(), errorResponseBuilder.getMessage());
      errorConsumer.ifPresent(c -> c.accept(throwable));
      return defaultValue;
    }
    return checkForAnyError();
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
  public Optional<T> onStatusCodeError() {
    if (throwable != null) {
      logger.warn("Exception happened but was intendedly ignored: {} ({})", throwable.toString(), errorResponseBuilder.getMessage());
      errorConsumer.ifPresent(c -> c.accept(throwable));
      return defaultValue;
    }
    return checkForStatusCodeError();
  }

}
