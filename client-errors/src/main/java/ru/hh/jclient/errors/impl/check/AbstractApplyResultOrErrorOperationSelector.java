package ru.hh.jclient.errors.impl.check;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.ws.rs.core.Response.Status;
import ru.hh.jclient.common.ResultOrErrorWithStatus;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.errors.impl.OperationBase;
import ru.hh.jclient.errors.impl.PredicateWithStatus;

public abstract class AbstractApplyResultOrErrorOperationSelector<T,
    D extends AbstractOperationSelector<E, D>,
    AO extends OperationBase<AO>,
    E>
    extends AbstractOperationSelector<E, D> {

  protected ResultOrErrorWithStatus<T, E> resultOrErrorWithStatus;

  public AbstractApplyResultOrErrorOperationSelector(ResultOrErrorWithStatus<T, E> resultOrErrorWithStatus, String errorMessage, Object... params) {
    super(errorMessage, params);
    this.resultOrErrorWithStatus = resultOrErrorWithStatus;
  }

  protected abstract AO createOperation(ResultOrErrorWithStatus<T, E> wrapper,
      Optional<Integer> errorStatusCode,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<E>> predicates,
      Optional<T> defaultValue);

  public AO throwBadGateway() {
    return createOperation(resultOrErrorWithStatus, of(HttpStatuses.BAD_GATEWAY), errorMessage, predicates, empty());
  }

  public AO throwGatewayTimeout() {
    return createOperation(resultOrErrorWithStatus, of(HttpStatuses.GATEWAY_TIMEOUT), errorMessage, predicates, empty());
  }

  public AO throwInternalServerError() {
    return createOperation(resultOrErrorWithStatus, of(INTERNAL_SERVER_ERROR.getStatusCode()), errorMessage, predicates, empty());
  }

  public AO throwConflict() {
    return createOperation(resultOrErrorWithStatus, of(CONFLICT.getStatusCode()), errorMessage, predicates, empty());
  }

  public AO throwForbidden() {
    return createOperation(resultOrErrorWithStatus, of(FORBIDDEN.getStatusCode()), errorMessage, predicates, empty());
  }

  public AO throwNotFound() {
    return createOperation(resultOrErrorWithStatus, of(NOT_FOUND.getStatusCode()), errorMessage, predicates, empty());
  }

  public AO throwBadRequest() {
    return createOperation(resultOrErrorWithStatus, of(BAD_REQUEST.getStatusCode()), errorMessage, predicates, empty());
  }

  /**
   * Uses status code from {@link ResultOrErrorWithStatus#getStatusCode()} (response code from remote call). Note that
   * {@link Status#SERVICE_UNAVAILABLE} will be replaced with {@link Status#BAD_GATEWAY}.
   */
  public AO proxyStatusCode() {
    return createOperation(resultOrErrorWithStatus, empty(), errorMessage, predicates, empty());
  }
}
