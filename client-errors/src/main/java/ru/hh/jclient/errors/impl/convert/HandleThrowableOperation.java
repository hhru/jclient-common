package ru.hh.jclient.errors.impl.convert;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.ws.rs.WebApplicationException;
import com.google.common.base.Throwables;
import ru.hh.jclient.errors.impl.OperationBase;

/**
 * Contains useful methods to handle error outcome of {@link CompletableFuture} with different jclient-common wrappers.
 */
public class HandleThrowableOperation<T> extends OperationBase<HandleThrowableOperation<T>> {

  private T result;
  private Throwable throwable;

  public HandleThrowableOperation(T result, Throwable exception, Integer errorStatusCode, Supplier<String> errorMessage) {
    super(Optional.of(errorStatusCode), errorMessage);
    this.result = result;
    this.throwable = exception;
  }

  /**
   * <p>
   * Convert source exception to {@link WebApplicationException} with predefined status code if matched ({@link Class#isAssignableFrom(Class)}) to one
   * of specified exception classes.
   * </p>
   * <p>
   * If source exception is wrapped in {@link CompletionException} it will be unwrapped. If source exception is instance of WAE it will not be
   * converted. Unmatched exception will be re-thrown using {@link Throwables#propagate(Throwable)}.
   * </p>
   * <p>
   * If there is no exception, return result as-is.
   * </p>
   *
   * @param exceptionClasses
   *          exception classes to match against provided exception
   * @return result if exception is null, otherwise throw exception, converted (if matched) or original
   */
  @SafeVarargs
  public final T on(Class<? extends Throwable>... exceptionClasses) {
    // do nothing if there is no exception
    if (throwable == null) {
      return result;
    }

    // extract cause if applicable
    Throwable converted = throwable instanceof CompletionException ? throwable.getCause() : throwable;

    // re-throw immediately if exception is WAE, we don't want to convert what is already converted
    if (converted instanceof WebApplicationException) {
      throw (WebApplicationException) converted;
    }

    // convert to WAE if one of specified classes supers provided (converted) throwable
    if (Stream.of(exceptionClasses).anyMatch(exceptionClass -> exceptionClass.isAssignableFrom(converted.getClass()))) {
      throw toException("was " + converted.getClass().getName() + ": " + converted.getMessage());
    }

    throw Throwables.propagate(throwable);
  }

  /**
   * <p>
   * Convert source exception to {@link WebApplicationException} with predefined status code.
   * </p>
   * <p>
   * If source exception is wrapped in {@link CompletionException} it will be unwrapped. If source exception is instance of WAE it will not be
   * converted.
   * </p>
   * <p>
   * If there is no exception, return result as-is.
   * </p>
   */
  public final T onAnyError() {
    return on(Throwable.class);
  }
}
