package ru.hh.jclient.common;

import java.util.concurrent.CompletableFuture;
import ru.hh.jclient.common.responseconverter.TypeConverter;
import ru.hh.jclient.common.util.SimpleRange;

public class EmptyOrErrorProcessor<E> extends ResultOrErrorProcessor<Void, E> {

  EmptyOrErrorProcessor(EmptyResultProcessor responseProcessor, TypeConverter<E> errorConverter) {
    super(responseProcessor, errorConverter);
  }

  public CompletableFuture<EmptyOrErrorWithStatus<E>> emptyWithStatus() {
    return resultWithResponse()
        .thenApply(ResultOrErrorWithResponse::hideResponse)
        .thenApply(rews -> new EmptyOrErrorWithStatus<>(rews.getError().orElse(null), rews.getStatusCode()));
  }

  /**
   * @deprecated Use {@link #emptyWithStatus()}
   */
  @Override
  @Deprecated // use #emptyWithStatus()
  public CompletableFuture<ResultOrErrorWithStatus<Void, E>> resultWithStatus() {
    return super.resultWithStatus();
  }

  @Override
  public EmptyOrErrorProcessor<E> forStatus(int status) {
    return (EmptyOrErrorProcessor<E>) super.forStatus(status);
  }

  public EmptyOrErrorProcessor<E> forStatus(SimpleRange statusCodes) {
    return (EmptyOrErrorProcessor<E>) super.forStatus(statusCodes);
  }
}
