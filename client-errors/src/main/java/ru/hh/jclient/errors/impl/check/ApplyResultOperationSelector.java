package ru.hh.jclient.errors.impl.check;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.ws.rs.core.Response.Status;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.common.HttpStatuses;

public class ApplyResultOperationSelector<T> extends AbstractOperationSelector<T, ApplyResultOperationSelector<T>> {

  protected ResultWithStatus<T> resultWithStatus;
  private List<Integer> proxiedStatusCodes;
  private Function<Integer, Integer> statusCodesConverter;

  public ApplyResultOperationSelector(ResultWithStatus<T> resultWithStatus, String errorMessage, Object... params) {
    super(errorMessage, params);
    this.resultWithStatus = resultWithStatus;
  }

  /**
   * <p>
   * Specifies what statuses to proxy in case of error. All other statuses will be converted to specified THROW_* status. Calling this method second
   * time will override previous value.
   * </p>
   * <code>
   * .thenApply(rws -> check(rws, "failed to get vacancy")<b>.proxyOnly(400, 404)</b>.throwForbidden().onAnyError();
   * </code>
   * <p>
   * This will throw WAE with 400 or 404 status if response status is the same, otherwise 403.
   * </p>
   */
  public ApplyResultOperationSelector<T> proxyOnly(Integer... statuses) {
    proxiedStatusCodes = Arrays.asList(statuses);
    return getSelf();
  }

  /**
   * <p>
   * Specifies what statuses to proxy in case of error. All other statuses will be converted to specified THROW_* status. Calling this method second
   * time will override previous value.
   * </p>
   * <code>
   * .thenApply(rws -> check(rws, "failed to get vacancy")<b>.proxyOnly(BAD_REQUEST, NOT_FOUND)</b>.throwForbidden().onAnyError();
   * </code>
   * <p>
   * This will throw WAE with 400 or 404 status if response status is the same, otherwise 403.
   * </p>
   */
  public ApplyResultOperationSelector<T> proxyOnly(Status... statuses) {
    proxiedStatusCodes = Stream.of(statuses).map(Status::getStatusCode).collect(toList());
    return getSelf();
  }

  /**
   * <p>
   * Specifies how matching response status code must be converted in case of error. If status code is converted it will be proxied (overriding
   * THROW_* status code). This method can be called multiple times and will chain conversion in the corresponding order.
   * </p>
   * <code>
   * .thenApply(rws -> check(rws, "failed to get vacancy")<b>.convertAndProxy(400, 409)</b>.throwForbidden().onAnyError();
   * </code>
   * <p>
   * This will throw WAE with 409 if response status is 400, otherwise 403.
   * </p>
   */
  public ApplyResultOperationSelector<T> convertAndProxy(Integer source, Integer target) {
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
   * THROW_* status code). This method can be called multiple times and will chain conversion in the corresponding order.
   * </p>
   * <code>
   * .thenApply(rws -> check(rws, "failed to get vacancy")<b>.convertAndProxy(BAD_REQUEST, CONFLICT)</b>.throwForbidden().onAnyError();
   * </code>
   * <p>
   * This will throw WAE with 409 if response status is 400, otherwise 403.
   * </p>
   */
  public ApplyResultOperationSelector<T> convertAndProxy(Status source, Status target) {
    return convertAndProxy(source.getStatusCode(), target.getStatusCode());
  }

  /**
   * Uses {@link Status#INTERNAL_SERVER_ERROR} status code.
   */
  public ApplyResultOperation<T> throwInternalServerError() {
    return new ApplyResultOperation<>(
        resultWithStatus,
        of(INTERNAL_SERVER_ERROR.getStatusCode()),
        ofNullable(proxiedStatusCodes),
        ofNullable(statusCodesConverter),
        errorMessage,
        predicates);
  }

  /**
   * Uses {@link Status#BAD_GATEWAY} status code.
   */
  public ApplyResultOperation<T> throwBadGateway() {
    return new ApplyResultOperation<>(
        resultWithStatus,
        of(HttpStatuses.BAD_GATEWAY),
        ofNullable(proxiedStatusCodes),
        ofNullable(statusCodesConverter),
        errorMessage,
        predicates);
  }

  /**
   * Uses {@link Status#FORBIDDEN} status code.
   */
  public ApplyResultOperation<T> throwForbidden() {
    return new ApplyResultOperation<>(
        resultWithStatus,
        of(FORBIDDEN.getStatusCode()),
        ofNullable(proxiedStatusCodes),
        ofNullable(statusCodesConverter),
        errorMessage,
        predicates);
  }

  /**
   * Uses {@link Status#NOT_FOUND} status code.
   */
  public ApplyResultOperation<T> throwNotFound() {
    return new ApplyResultOperation<>(
        resultWithStatus,
        of(NOT_FOUND.getStatusCode()),
        ofNullable(proxiedStatusCodes),
        ofNullable(statusCodesConverter),
        errorMessage,
        predicates);
  }

  /**
   * Uses {@link Status#BAD_REQUEST} status code.
   */
  public ApplyResultOperation<T> throwBadRequest() {
    return new ApplyResultOperation<>(
        resultWithStatus,
        of(BAD_REQUEST.getStatusCode()),
        ofNullable(proxiedStatusCodes),
        ofNullable(statusCodesConverter),
        errorMessage,
        predicates);
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
  public ApplyResultOperation<T> proxyStatusCode() {
    return new ApplyResultOperation<>(resultWithStatus, empty(), empty(), empty(), errorMessage, predicates);
  }

  /**
   * <p>
   * Sets default value to return.
   * </p>
   * <p>
   * Calling {@link #proxyOnly(Status...)} or {@link #convertAndProxy(Status, Status)} does nothing when used with this operation.
   * </p>
   *
   * @param value
   *          default value to return in case of error
   */
  public ApplyResultOperation<T> returnDefault(T value) {
    return new ApplyResultOperation<>(resultWithStatus, empty(), empty(), empty(), errorMessage, predicates, of(value));
  }

  /**
   * <p>
   * Sets default value to return.
   * </p>
   * <p>
   * Calling {@link #proxyOnly(Status...)} or {@link #convertAndProxy(Status, Status)} does nothing when used with this operation.
   * </p>
   *
   * @param value
   *          default value to return in case of error
   */
  public ApplyResultOperation<T> returnDefault(Supplier<T> value) {
    return new ApplyResultOperation<>(resultWithStatus, empty(), empty(), empty(), errorMessage, predicates, of(value.get()));
  }
}
