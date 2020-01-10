package ru.hh.jclient.errors.impl.check;

import javax.ws.rs.core.Response.Status;
import ru.hh.jclient.common.EmptyOrErrorWithStatus;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.GATEWAY_TIMEOUT;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

public class ApplyEmptyResultOrErrorOperationSelector<E> extends AbstractOperationSelector<E, ApplyEmptyResultOrErrorOperationSelector<E>> {

  private EmptyOrErrorWithStatus<E> emptyOrErrorWithStatus;

  public ApplyEmptyResultOrErrorOperationSelector(EmptyOrErrorWithStatus<E> emptyOrErrorWithStatus, String errorMessage, Object... params) {
    super(errorMessage, params);
    this.emptyOrErrorWithStatus = emptyOrErrorWithStatus;
  }

  /**
   * <p>
   * Specifies empty value to return if error is present.
   * </p>
   */
  public ApplyEmptyResultOrErrorOperation<E> returnEmpty() {
    return new ApplyEmptyResultOrErrorOperation<>(emptyOrErrorWithStatus, empty(), errorMessage, predicates, true);
  }

  private ApplyEmptyResultOrErrorOperation<E> throwWithCode(int code) {
    return new ApplyEmptyResultOrErrorOperation<>(emptyOrErrorWithStatus, of(code), errorMessage, predicates, false);
  }

  public ApplyEmptyResultOrErrorOperation<E> throwBadGateway() {
    return throwWithCode(BAD_GATEWAY.getStatusCode());
  }

  public ApplyEmptyResultOrErrorOperation<E> throwGatewayTimeout() {
    return throwWithCode(GATEWAY_TIMEOUT.getStatusCode());
  }

  public ApplyEmptyResultOrErrorOperation<E> throwInternalServerError() {
    return throwWithCode(INTERNAL_SERVER_ERROR.getStatusCode());
  }

  public ApplyEmptyResultOrErrorOperation<E> throwConflict() {
    return throwWithCode(CONFLICT.getStatusCode());
  }

  public ApplyEmptyResultOrErrorOperation<E> throwForbidden() {
    return throwWithCode(FORBIDDEN.getStatusCode());
  }

  public ApplyEmptyResultOrErrorOperation<E> throwNotFound() {
    return throwWithCode(NOT_FOUND.getStatusCode());
  }

  public ApplyEmptyResultOrErrorOperation<E> throwBadRequest() {
    return throwWithCode(BAD_REQUEST.getStatusCode());
  }

  /**
   * Uses status code from {@link EmptyOrErrorWithStatus#getStatusCode()} (response code from remote call). Note that
   * {@link Status#SERVICE_UNAVAILABLE} will be replaced with {@link Status#BAD_GATEWAY}.
   */
  public ApplyEmptyResultOrErrorOperation<E> proxyStatusCode() {
    return new ApplyEmptyResultOrErrorOperation<>(emptyOrErrorWithStatus, empty(), errorMessage, predicates, false);
  }
}
