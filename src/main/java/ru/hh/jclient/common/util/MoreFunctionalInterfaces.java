package ru.hh.jclient.common.util;

import java.util.Objects;
import java.util.function.Consumer;

public class MoreFunctionalInterfaces {
  
  /**
   * Same as {@link Consumer} but {@link #accept(Object)} throws exception.
   */
  @FunctionalInterface
  public interface ThrowableConsumer<T, E extends Exception> {
    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     * @throws exception described in type
     */
    void accept(T t) throws E;

    /**
     * Returns a composed {@code Consumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code Consumer} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default ThrowableConsumer<T, E> andThen(ThrowableConsumer<? super T, E> after) {
        Objects.requireNonNull(after);
        return (T t) -> { accept(t); after.accept(t); };
    }
  }

  @FunctionalInterface
  public interface ThrowableSupplier<T, E extends Exception> {
    /**
     * Gets a result.
     *
     * @return a result
     * @throws exception described in type
     */
    T get() throws E;
  }

}
