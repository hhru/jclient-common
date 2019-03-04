package ru.hh.jclient.errors.impl.check;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import java.util.function.Supplier;
import javax.ws.rs.core.Response.Status;
import ru.hh.jclient.common.ResultOrErrorWithStatus;
import ru.hh.jclient.common.HttpStatuses;

public class ApplyResultOrErrorOperationSelector<T, E> extends AbstractOperationSelector<E, ApplyResultOrErrorOperationSelector<T, E>> {

  private ResultOrErrorWithStatus<T, E> resultOrErrorWithStatus;

  public ApplyResultOrErrorOperationSelector(ResultOrErrorWithStatus<T, E> resultOrErrorWithStatus, String errorMessage, Object... params) {
    super(errorMessage, params);
    this.resultOrErrorWithStatus = resultOrErrorWithStatus;
  }

  /**
   * Specifies default value to set to result if error is present.
   *
   * @param defaultValue
   *          default value to set
   */
  public ApplyResultOrErrorOperation<T, E> setDefault(T defaultValue) {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, empty(), errorMessage, predicates, of(defaultValue));
  }

  /**
   * Specifies default value to set to result if error is present.
   *
   * @param defaultValue
   *          default value to set
   */
  public ApplyResultOrErrorOperation<T, E> setDefault(Supplier<T> defaultValue) {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, empty(), errorMessage, predicates, of(defaultValue.get()));
  }

  public ApplyResultOrErrorOperation<T, E> throwBadGateway() {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, of(HttpStatuses.BAD_GATEWAY), errorMessage, predicates, empty());
  }

  public ApplyResultOrErrorOperation<T, E> throwGatewayTimeout() {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, of(HttpStatuses.GATEWAY_TIMEOUT), errorMessage, predicates, empty());
  }

  public ApplyResultOrErrorOperation<T, E> throwInternalServerError() {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, of(INTERNAL_SERVER_ERROR.getStatusCode()), errorMessage, predicates, empty());
  }

  public ApplyResultOrErrorOperation<T, E> throwConflict() {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, of(CONFLICT.getStatusCode()), errorMessage, predicates, empty());
  }

  public ApplyResultOrErrorOperation<T, E> throwForbidden() {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, of(FORBIDDEN.getStatusCode()), errorMessage, predicates, empty());
  }

  public ApplyResultOrErrorOperation<T, E> throwNotFound() {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, of(NOT_FOUND.getStatusCode()), errorMessage, predicates, empty());
  }

  public ApplyResultOrErrorOperation<T, E> throwBadRequest() {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, of(BAD_REQUEST.getStatusCode()), errorMessage, predicates, empty());
  }

  /**
   * Uses status code from {@link ResultOrErrorWithStatus#getStatusCode()} (response code from remote call). Note that
   * {@link Status#SERVICE_UNAVAILABLE} will be replaced with {@link Status#BAD_GATEWAY}.
   */
  public ApplyResultOrErrorOperation<T, E> proxyStatusCode() {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, empty(), errorMessage, predicates, empty());
  }
}
