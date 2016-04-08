package ru.hh.jclient.errors;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;
import javax.ws.rs.WebApplicationException;
import com.google.common.base.Throwables;

/**
 * Contains useful methods to handle error outcome of {@link CompletableFuture} with different jclient-common wrappers.
 */
public class ClientExceptionHandler<T> extends AbstractErrorHandlerBase<ClientExceptionHandler<T>> {

  private T result;
  private Throwable throwable;

  ClientExceptionHandler(T result, Throwable exception, Integer errorStatusCode, String errorMessage) {
    super(Optional.of(errorStatusCode), errorMessage);
    this.result = result;
    this.throwable = exception;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Class<ClientExceptionHandler<T>> getDerivedClass() {
    return (Class<ClientExceptionHandler<T>>) getClass();
  }

  /**
   * Convert exception to {@link WebApplicationException} with predefined status code. Please note that WebApplicationException will not be converted.
   * If provided exception is wrapped in {@link CompletionException} it will be unwrapped. Unmatched exception will be re-thrown (runtime as-is,
   * otherwise wrapped in runtime - see {@link Throwables#propagate(Throwable)}).
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
