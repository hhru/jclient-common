package ru.hh.jclient.errors.impl.check;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.ws.rs.core.Response.Status;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.ResultWithStatus;

public class CheckedResultWithStatus<T> {
  private Optional<T> value;
  private final int status;

  public CheckedResultWithStatus(ResultWithStatus<T> result) {
    this.value = result.get();
    this.status = result.getStatusCode();
  }

  public CheckedResultWithStatus(Optional<T> value, int status) {
    this.value = value;
    this.status = status;
  }

  public <R> R mapOrElse(Function<T, R> mapper, Function<Integer, R> orElseMapper) {
    return value.map(mapper).orElseGet(() -> orElseMapper.apply(status));
  }

  public <R> CheckedResultWithStatus<R> map(Function<T, R> mapper) {
    return new CheckedResultWithStatus<>(value.map(mapper), status);
  }

  public <R> CheckedResultWithStatus<R> map(BiFunction<T, Integer, R> mapper) {
    return new CheckedResultWithStatus<>(value.map(v -> mapper.apply(v, status)), status);
  }

  public CheckedResultWithStatus<T> ifPresent(Consumer<T> consumer) {
    return ifPresent((value, status) -> consumer.accept(value));
  }

  public CheckedResultWithStatus<T> ifPresent(BiConsumer<T, Integer> consumer) {
    value.ifPresent(v -> consumer.accept(v, status));
    return this;
  }

  public CheckedResultWithStatus<T> onSuccessStatus(BiConsumer<T, Integer> consumer) {
    if (HttpClient.OK_RANGE.contains(status)) {
      consumer.accept(value.orElse(null), status);
    }
    return this;
  }

  public CheckedResultWithStatus<T> onSuccessStatus(Consumer<T> consumer) {
    return onSuccessStatus((value, status) -> consumer.accept(value));
  }

  public CheckedResultWithStatus<T> onFailureStatus(Runnable runnable) {
    return onFailureStatus(status -> runnable.run());
  }

  public CheckedResultWithStatus<T> onFailureStatus(Consumer<Integer> consumer) {
    if (!HttpClient.OK_RANGE.contains(status)) {
      consumer.accept(status);
    }
    return this;
  }

  public CheckedResultWithStatus<T> onStatus(Status status, Consumer<T> consumer) {
    return onStatus(status.getStatusCode(), consumer);
  }

  public CheckedResultWithStatus<T> onStatus(int status, Consumer<T> consumer) {
    if (this.status == status) {
      consumer.accept(value.orElse(null));
    }
    return this;
  }

  public T orElse(Function<Integer, T> supplier) {
    return value.orElseGet(() -> supplier.apply(status));
  }

  public T orElse(T t) {
    return value.orElse(t);
  }

  public T orElse(Supplier<T> supplier) {
    return value.orElseGet(supplier);
  }

  public T get() {
    return value.get();
  }

  public int getStatus() {
    return status;
  }
}
