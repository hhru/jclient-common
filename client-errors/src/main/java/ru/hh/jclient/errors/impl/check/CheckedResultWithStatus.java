package ru.hh.jclient.errors.impl.check;

import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.Response.Status;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.ResultWithStatus;

public class CheckedResultWithStatus<T> {

  @Nullable
  private final T value;
  private final int status;

  public CheckedResultWithStatus(ResultWithStatus<T> result) {
    this.value = result.get().orElse(null);
    this.status = result.getStatusCode();
  }

  public CheckedResultWithStatus(@Nullable T value, int status) {
    this.value = value;
    this.status = status;
  }

  public <R> R mapOrElse(Function<T, R> mapper, Function<Integer, R> orElseMapper) {
    return value != null ? mapper.apply(value) : orElseMapper.apply(status);
  }

  public <R> CheckedResultWithStatus<R> map(Function<T, R> mapper) {
    return new CheckedResultWithStatus<>(
        value != null ? mapper.apply(value) : null,
        status
    );
  }

  public <R> CheckedResultWithStatus<R> map(BiFunction<T, Integer, R> mapper) {
    return new CheckedResultWithStatus<>(
        value != null ? mapper.apply(value, status) : null,
        status
    );
  }

  public CheckedResultWithStatus<T> ifPresent(Consumer<T> consumer) {
    return ifPresent((value, status) -> consumer.accept(value));
  }

  public CheckedResultWithStatus<T> ifPresent(BiConsumer<T, Integer> consumer) {
    if (value != null) {
      consumer.accept(value, status);
    }
    return this;
  }

  public CheckedResultWithStatus<T> onSuccessStatus(BiConsumer<T, Integer> consumer) {
    if (HttpClient.OK_RANGE.contains(status)) {
      consumer.accept(value, status);
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
      consumer.accept(value);
    }
    return this;
  }

  public T orElse(Function<Integer, T> supplier) {
    return value != null ? value : supplier.apply(status);
  }

  public T orElse(T t) {
    return value != null ? value : t;
  }

  public T orElse(Supplier<T> supplier) {
    return value != null ? value : supplier.get();
  }

  public T get() {
    return Objects.requireNonNull(value);
  }

  public int getStatus() {
    return status;
  }
}
