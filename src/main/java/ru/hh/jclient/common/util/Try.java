package ru.hh.jclient.common.util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class Try<V> {

  private final V success;
  private final Throwable failure;

  private Try(V success, Throwable failure) {
    this.success = success;
    this.failure = failure;
  }

  public boolean isSuccess() {
    return success != null;
  }

  public V get() {
    return success;
  }

  public boolean isFailure() {
    return failure != null;
  }

  public Throwable reasonOfFail() {
    return failure;
  }

  public static <V> Try<V> success(V value) {
    return new Try<V>(value, null);
  }

  public static <V> Try<V> failure(Throwable failure) {
    return new Try<V>(null, failure);
  }

  @Override
  public String toString() {
    if (isSuccess()) {
      return "Success(" + success + ")";
    }
    return "Failure(" + failure + ")";
  }

  public static <V> Try<V> of(Callable<V> action) {
    try {
      return success(action.call());
    }
    catch (Throwable t) {
      return failure(t);
    }
  }

  @SuppressWarnings("unchecked")
  public <B> Try<B> map(Function<V, B> mappingFunction) {
    if (isSuccess()) {
      try {
        return success(mappingFunction.apply(get()));
      }
      catch (Throwable t) {
        return failure(t);
      }
    }
    return (Try<B>) this;
  }

  @SuppressWarnings("unchecked")
  public <B> Try<B> map(Function<V, B> mappingFunction, Consumer<V> finalizer) {
    if (isSuccess()) {
      try {
        return success(mappingFunction.apply(get()));
      }
      catch (Throwable t) {
        return failure(t);
      }
      finally {
        try {
          finalizer.accept(get());
        }
        catch (Throwable t2) {
          return failure(t2);
        }
      }
    }
    return (Try<B>) this;
  }
}
