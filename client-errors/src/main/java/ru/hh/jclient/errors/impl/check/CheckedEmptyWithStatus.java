package ru.hh.jclient.errors.impl.check;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.ResultWithStatus;

public class CheckedEmptyWithStatus {
  private int status;

  public CheckedEmptyWithStatus(ResultWithStatus<?> emptyWithStatus) {
    this.status = emptyWithStatus.getStatusCode();
  }

  public <T> CheckedResultWithStatus<T> map(Supplier<T> supplier) {
    return map(status -> supplier.get());
  }

  public <T> CheckedResultWithStatus<T> map(Function<Integer, T> mapper) {
    if (HttpClient.OK_RANGE.contains(status)) {
      return new CheckedResultWithStatus<>(Optional.ofNullable(mapper.apply(status)), status);
    }
    return new CheckedResultWithStatus<>(Optional.empty(), status);
  }

  public int getStatus() {
    return status;
  }
}
