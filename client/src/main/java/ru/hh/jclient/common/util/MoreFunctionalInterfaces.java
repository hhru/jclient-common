package ru.hh.jclient.common.util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MoreFunctionalInterfaces {
  
  /**
   * Same as {@link Consumer} but {@link #accept(Object)} throws an exception.
   */
  @FunctionalInterface
  public interface FailableConsumer<T, E extends Throwable> {
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
    default FailableConsumer<T, E> andThen(FailableConsumer<? super T, E> after) {
        Objects.requireNonNull(after);
        return (T t) -> { accept(t); after.accept(t); };
    }
  }

  /**
   * {@inheritDoc}
   */
  @FunctionalInterface
  public interface ThrowableConsumer<T> extends FailableConsumer<T, Throwable> {
  }

  /**
   * Same as {@link Supplier} but {@link #get()} throws an exception.
   */
  @FunctionalInterface
  public interface FailableSupplier<T, E extends Throwable> {
    /**
     * Gets a result.
     *
     * @return a result
     * @throws exception described in type
     */
    T get() throws E;
  }

  /**
   * {@inheritDoc}
   */
  @FunctionalInterface
  public interface ThrowableSupplier<T> extends FailableSupplier<T, Throwable> {
  }

  /**
   * Same as {@link Function} but {@link #apply(Object)} throws an exception.
   */
  @FunctionalInterface
  public interface FailableFunction<T, R, E extends Throwable> {

    /**
     * Applies this function to the given argument.
     *
     * @param t
     *          the function argument
     * @return the function result
     */
    R apply(T t) throws E;

    /**
     * Returns a composed function that first applies the {@code before} function to its input, and then applies this function to the result. If
     * evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     *
     * @param <V>
     *          the type of input to the {@code before} function, and to the composed function
     * @param before
     *          the function to apply before this function is applied
     * @return a composed function that first applies the {@code before} function and then applies this function
     * @throws NullPointerException
     *           if before is null
     *
     * @see #andThen(Function)
     */
    default <V> FailableFunction<V, R, E> compose(FailableFunction<? super V, ? extends T, E> before) {
      Objects.requireNonNull(before);
      return (V v) -> apply(before.apply(v));
    }

    /**
     * Returns a composed function that first applies this function to its input, and then applies the {@code after} function to the result. If
     * evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     *
     * @param <V>
     *          the type of output of the {@code after} function, and of the composed function
     * @param after
     *          the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after} function
     * @throws NullPointerException
     *           if after is null
     *
     * @see #compose(Function)
     */
    default <V> FailableFunction<T, V, E> andThen(FailableFunction<? super R, ? extends V, E> after) {
      Objects.requireNonNull(after);
      return (T t) -> after.apply(apply(t));
    }

    /**
     * Returns a function that always returns its input argument.
     *
     * @param <T>
     *          the type of the input and output objects to the function
     * @return a function that always returns its input argument
     */
    static <T, E extends Throwable> FailableFunction<T, T, E> identity() {
      return t -> t;
    }
  }

  /**
   * {@inheritDoc}
   */
  @FunctionalInterface
  public interface ThrowableFunction<T, R> extends FailableFunction<T, R, Throwable> {
  }

}
