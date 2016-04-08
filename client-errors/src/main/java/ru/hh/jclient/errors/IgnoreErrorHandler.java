package ru.hh.jclient.errors;

import static java.util.Optional.empty;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import ru.hh.jclient.common.ResultWithStatus;

/**
 * Contains useful methods to handle error outcome of {@link CompletableFuture} with different jclient-common wrappers.
 */
public class IgnoreErrorHandler<T> extends AbstractErrorHandler<T, IgnoreErrorHandler<T>> {

  private Throwable throwable;
  private Optional<Consumer<Throwable>> errorConsumer;

  IgnoreErrorHandler(
      ResultWithStatus<T> wrapper,
      Throwable throwable,
      String errorMessage,
      Optional<T> defaultValue,
      Optional<Consumer<Throwable>> errorConsumer) {
    super(wrapper, empty(), empty(), empty(), errorMessage, defaultValue);
    this.throwable = throwable;
    this.errorConsumer = errorConsumer;
  }

  @Override
  protected boolean useDefault() {
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Class<IgnoreErrorHandler<T>> getDerivedClass() {
    return (Class<IgnoreErrorHandler<T>>) getClass();
  }

  /**
   * Returns Optional with default value (if specified) or empty if
   *
   * <ul>
   * <li>exception is not null</li>
   * <li>result is incorrect according to {@link InvalidResultHandler#onAnyError()}</li>
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
    Optional<T> result = checkForAnyError();
    return result;
  }

}
