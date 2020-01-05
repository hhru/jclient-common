package ru.hh.jclient.common;

import java.util.concurrent.CompletableFuture;
import ru.hh.jclient.common.responseconverter.TypeConverter;

public class EmptyResultProcessor extends ResultProcessor<Void> {

  EmptyResultProcessor(HttpClient httpClient, TypeConverter<Void> converter) {
    super(httpClient, converter);
  }

  public CompletableFuture<EmptyWithStatus> emptyWithStatus() {
    return super.resultWithStatus().thenApply(rws -> (EmptyWithStatus)rws);
  }

  /**
   * @deprecated Use {@link #emptyWithStatus()}
   */
  @Override
  @Deprecated // use #emptyWithStatus()
  public CompletableFuture<ResultWithStatus<Void>> resultWithStatus() {
    return super.resultWithStatus();
  }
}
