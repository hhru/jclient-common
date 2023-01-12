package ru.hh.jclient.errors.impl.check;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
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
    value.ifPresent(consumer);
    return this;
  }

  public CheckedResultWithStatus<T> onSuccess(Consumer<T> consumer) {
    if (HttpClient.OK_RANGE.contains(status)) {
      value.ifPresent(consumer);
    }
    return this;
  }

  public CheckedResultWithStatus<T> onFailure(Runnable runnable) {
    if (!HttpClient.OK_RANGE.contains(status)) {
      runnable.run();
    }
    return this;
  }

  public CheckedResultWithStatus<T> onStatus(Status status, Runnable runnable) {
    return onStatus(status.getStatusCode(), runnable);
  }

  public CheckedResultWithStatus<T> onStatus(int status, Runnable runnable) {
    if (this.status == status) {
      runnable.run();
    }
    return this;
  }

  public T orElse(Function<Integer, T> supplier) {
    return value.orElseGet(() -> supplier.apply(status));
  }

  public Optional<T> get() {
    return value;
  }

  public int getStatus() {
    return status;
  }
}
