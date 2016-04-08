package ru.hh.jclient.errors.impl.convert;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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

  public HandleThrowableOperation(T result, Throwable exception, Integer errorStatusCode, String errorMessage) {
    super(Optional.of(errorStatusCode), errorMessage);
    this.result = result;
    this.throwable = exception;
  }

  /**
   * Convert exception to {@link WebApplicationException} with predefined status code. If source exception is already instance of WAE it will not be
   * converted. If provided exception is wrapped in {@link CompletionException} it will be unwrapped. Unmatched exception will be re-thrown (runtime
   * as-is, otherwise wrapped in runtime - see {@link Throwables#propagate(Throwable)}).
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

  public final T onAnyError() {
    return on(Throwable.class);
  }
}
