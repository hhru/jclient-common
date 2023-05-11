package ru.hh.jclient.errors.impl.check;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import javax.ws.rs.core.Response.Status;
import static javax.ws.rs.core.Response.Status.BAD_GATEWAY;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import ru.hh.jclient.common.EmptyWithStatus;
import ru.hh.jclient.common.ResultWithStatus;

public class ApplyEmptyResultOperationSelector extends AbstractOperationSelector<Void, ApplyEmptyResultOperationSelector> {


  protected EmptyWithStatus emptyWithStatus;
  private List<Integer> proxiedStatusCodes = List.of();
  private Function<Integer, Integer> statusCodesConverter;

  public ApplyEmptyResultOperationSelector(EmptyWithStatus emptyWithStatus, String errorMessage, Object... params) {
    super(errorMessage, params);
    this.emptyWithStatus = emptyWithStatus;
  }

  /**
   * <p>
   * Specifies what statuses to proxy in case of error. All other statuses will be converted to specified throw_* status. Calling this method second
   * time will override previous value.
   * </p>
   * <code>
   * .thenApply(rws -> check(rws, "failed to get vacancy")<b>.proxyOnly(400, 404)</b>.throwForbidden().onAnyError();
   * </code>
   * <p>
   * This will throw WAE with 400 or 404 status if response status is the same, otherwise 403.
   * </p>
   */
  public ApplyEmptyResultOperationSelector proxyOnly(Integer... statuses) {
    proxiedStatusCodes = Arrays.asList(statuses);
    return getSelf();
  }

  /**
   * <p>
   * Specifies what statuses to proxy in case of error. All other statuses will be converted to specified throw_* status. Calling this method second
   * time will override previous value.
   * </p>
   * <code>
   * .thenApply(rws -> check(rws, "failed to get vacancy")<b>.proxyOnly(BAD_REQUEST, NOT_FOUND)</b>.throwForbidden().onAnyError();
   * </code>
   * <p>
   * This will throw WAE with 400 or 404 status if response status is the same, otherwise 403.
   * </p>
   */
  public ApplyEmptyResultOperationSelector proxyOnly(Status... statuses) {
    proxiedStatusCodes = Stream.of(statuses).map(Status::getStatusCode).collect(toList());
    return getSelf();
  }

  /**
   * <p>
   * Specifies how matching response status code must be converted in case of error. If status code is converted it will be proxied (overriding
   * throw_* status code). This method can be called multiple times and will chain conversion in the corresponding order.
   * </p>
   * <code>
   * .thenApply(rws -> check(rws, "failed to get vacancy")<b>.convertAndProxy(400, 409)</b>.throwForbidden().onAnyError();
   * </code>
   * <p>
   * This will throw WAE with 409 if response status is 400, otherwise 403.
   * </p>
   */
  public ApplyEmptyResultOperationSelector convertAndProxy(Integer source, Integer target) {
    Function<Integer, Integer> converter = i -> i.equals(source) ? target : i;
    if (statusCodesConverter == null) {
      statusCodesConverter = converter;
    }
    else {
      statusCodesConverter = statusCodesConverter.andThen(converter);
    }
    return getSelf();
  }

  /**
   * <p>
   * Specifies how matching response status code must be converted in case of error. If status code is converted it will be proxied (overriding
   * throw_* status code). This method can be called multiple times and will chain conversion in the corresponding order.
   * </p>
   * <code>
   * .thenApply(rws -> check(rws, "failed to get vacancy")<b>.convertAndProxy(BAD_REQUEST, CONFLICT)</b>.throwForbidden().onAnyError();
   * </code>
   * <p>
   * This will throw WAE with 409 if response status is 400, otherwise 403.
   * </p>
   */
  public ApplyEmptyResultOperationSelector convertAndProxy(Status source, Status target) {
    return convertAndProxy(source.getStatusCode(), target.getStatusCode());
  }

  private ApplyEmptyResultOperation throwWithCode(int code) {
    return new ApplyEmptyResultOperation(
        emptyWithStatus,
        code,
        proxiedStatusCodes,
        statusCodesConverter,
        errorMessage,
        predicates,
        allowedStatuses,
        exceptionBuilder
    );
  }

  /**
   * Uses {@link Status#INTERNAL_SERVER_ERROR} status code.
   */
  public ApplyEmptyResultOperation throwInternalServerError() {
    return throwWithCode(INTERNAL_SERVER_ERROR.getStatusCode());
  }

  /**
   * Uses {@link Status#BAD_GATEWAY} status code.
   */
  public ApplyEmptyResultOperation throwBadGateway() {
    return throwWithCode(BAD_GATEWAY.getStatusCode());
  }

  /**
   * Uses {@link Status#FORBIDDEN} status code.
   */
  public ApplyEmptyResultOperation throwForbidden() {
    return throwWithCode(FORBIDDEN.getStatusCode());
  }

  /**
   * Uses {@link Status#NOT_FOUND} status code.
   */
  public ApplyEmptyResultOperation throwNotFound() {
    return throwWithCode(NOT_FOUND.getStatusCode());
  }

  /**
   * Uses {@link Status#BAD_REQUEST} status code.
   */
  public ApplyEmptyResultOperation throwBadRequest() {
    return throwWithCode(BAD_REQUEST.getStatusCode());
  }

  /**
   * <p>
   * Uses status code from {@link ResultWithStatus#getStatusCode()} (response code from remote call). Note that {@link Status#SERVICE_UNAVAILABLE}
   * will be replaced with {@link Status#BAD_GATEWAY}.
   * </p>
   * <p>
   * Calling {@link #proxyOnly(Status...)} or {@link #convertAndProxy(Status, Status)} does nothing when used with this operation.
   * </p>
   */
  public ApplyEmptyResultOperation proxyStatusCode() {
    return new ApplyEmptyResultOperation(emptyWithStatus, null, List.of(), null, errorMessage, predicates, allowedStatuses, exceptionBuilder);
  }

  /**
   * <p>
   * Sets empty value to return.
   * </p>
   * <p>
   * Calling {@link #proxyOnly(Status...)} or {@link #convertAndProxy(Status, Status)} does nothing when used with this operation.
   * </p>
   */
  public ApplyEmptyResultOperation returnEmpty() {
    return new ApplyEmptyResultOperation(
        emptyWithStatus,
        null,
        List.of(),
        null,
        errorMessage,
        predicates,
        null,
        allowedStatuses,
        exceptionBuilder);
  }

}
