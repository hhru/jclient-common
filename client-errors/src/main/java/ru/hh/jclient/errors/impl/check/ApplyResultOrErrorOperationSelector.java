package ru.hh.jclient.errors.impl.check;

import jakarta.ws.rs.core.Response.Status;
import static jakarta.ws.rs.core.Response.Status.BAD_GATEWAY;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.GATEWAY_TIMEOUT;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import java.util.function.Supplier;
import ru.hh.jclient.common.ResultOrErrorWithStatus;

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
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, null, errorMessage, predicates, defaultValue);
  }

  /**
   * Specifies default null to set to result if error is present.
   *
   */
  public ApplyResultOrErrorOperation<T, E> setDefaultNull() {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, null, errorMessage, predicates, null);
  }

  /**
   * Specifies default value to set to result if error is present.
   *
   * @param defaultValue
   *          default value to set
   */
  public ApplyResultOrErrorOperation<T, E> setDefault(Supplier<T> defaultValue) {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, null, errorMessage, predicates, defaultValue.get());
  }

  private ApplyResultOrErrorOperation<T, E> throwWithCode(int code) {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, code, errorMessage, predicates);
  }

  public ApplyResultOrErrorOperation<T, E> throwBadGateway() {
    return throwWithCode(BAD_GATEWAY.getStatusCode());
  }

  public ApplyResultOrErrorOperation<T, E> throwGatewayTimeout() {
    return throwWithCode(GATEWAY_TIMEOUT.getStatusCode());
  }

  public ApplyResultOrErrorOperation<T, E> throwInternalServerError() {
    return throwWithCode(INTERNAL_SERVER_ERROR.getStatusCode());
  }

  public ApplyResultOrErrorOperation<T, E> throwConflict() {
    return throwWithCode(CONFLICT.getStatusCode());
  }

  public ApplyResultOrErrorOperation<T, E> throwForbidden() {
    return throwWithCode(FORBIDDEN.getStatusCode());
  }

  public ApplyResultOrErrorOperation<T, E> throwNotFound() {
    return throwWithCode(NOT_FOUND.getStatusCode());
  }

  public ApplyResultOrErrorOperation<T, E> throwBadRequest() {
    return throwWithCode(BAD_REQUEST.getStatusCode());
  }

  /**
   * Uses status code from {@link ResultOrErrorWithStatus#getStatusCode()} (response code from remote call). Note that
   * {@link Status#SERVICE_UNAVAILABLE} will be replaced with {@link Status#BAD_GATEWAY}.
   */
  public ApplyResultOrErrorOperation<T, E> proxyStatusCode() {
    return new ApplyResultOrErrorOperation<>(resultOrErrorWithStatus, null, errorMessage, predicates);
  }
}
