package ru.hh.jclient.errors.impl.convert;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import ru.hh.jclient.errors.MoreErrors;

public class HandleThrowableOperationSelector<T> {

  private T result;
  private Throwable throwable;
  private String errorMessage;

  public HandleThrowableOperationSelector(T result, Throwable throwable, String errorMessage) {
    this.result = result;
    this.throwable = throwable;
    this.errorMessage = errorMessage;
  }

  public HandleThrowableOperation<T> THROW_BAD_GATEWAY() {
    return new HandleThrowableOperation<>(result, throwable, MoreErrors.BAD_GATEWAY, errorMessage);
  }

  public HandleThrowableOperation<T> THROW_GATEWAY_TIMEOUT() {
    return new HandleThrowableOperation<>(result, throwable, MoreErrors.GATEWAY_TIMEOUT, errorMessage);
  }

  public HandleThrowableOperation<T> THROW_INTERNAL_SERVER_ERROR() {
    return new HandleThrowableOperation<>(result, throwable, INTERNAL_SERVER_ERROR.getStatusCode(), errorMessage);
  }
}