package ru.hh.jclient.errors.impl.convert;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.errors.impl.OperationSelectorBase;

public class HandleThrowableOperationSelector<T> extends OperationSelectorBase {
  private T result;
  private Throwable throwable;

  public HandleThrowableOperationSelector(T result, Throwable throwable, String errorMessage, Object... params) {
    super(errorMessage, params);
    this.result = result;
    this.throwable = throwable;
  }

  public HandleThrowableOperation<T> throwBadGateway() {
    return new HandleThrowableOperation<>(result, throwable, HttpStatuses.BAD_GATEWAY, errorMessage);
  }

  public HandleThrowableOperation<T> throwGatewayTimeout() {
    return new HandleThrowableOperation<>(result, throwable, HttpStatuses.GATEWAY_TIMEOUT, errorMessage);
  }

  public HandleThrowableOperation<T> throwInternalServerError() {
    return new HandleThrowableOperation<>(result, throwable, INTERNAL_SERVER_ERROR.getStatusCode(), errorMessage);
  }
}
