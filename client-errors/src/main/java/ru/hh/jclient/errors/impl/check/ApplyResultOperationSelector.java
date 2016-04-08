package ru.hh.jclient.errors.impl.check;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.ws.rs.core.Response.Status;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.MoreErrors;

public class ApplyResultOperationSelector<T> extends AbstractOperationSelector<T, ApplyResultOperationSelector<T>> {

  protected ResultWithStatus<T> resultWithStatus;
  private List<Integer> proxiedStatusCodes;
  private Function<Integer, Integer> statusCodesConverter;

  public ApplyResultOperationSelector(ResultWithStatus<T> resultWithStatus, String errorMessage) {
    super(errorMessage);
    this.resultWithStatus = resultWithStatus;
  }

  /**
   * Specifies what statuses to proxy. All other statuses will be converted to specified THROW_* status. Calling this method will override previous
   * value.
   */
  public ApplyResultOperationSelector<T> proxyOnly(Integer... statuses) {
    proxiedStatusCodes = Arrays.asList(statuses);
    return getSelf();
  }

  /**
   * Specifies what statuses to proxy. All other statuses will be converted to specified THROW_* status.
   */
  public ApplyResultOperationSelector<T> proxyOnly(Status... statuses) {
    proxiedStatusCodes = Stream.of(statuses).map(Status::getStatusCode).collect(toList());
    return getSelf();
  }

  /**
   * Specifies how matching response status code must be converted. Converting only happens for error cases. If status code is converted it will be
   * proxied (overriding THROW_* status code). This method can be called multiple times and will chain conversion in the corresponding order.
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
   * Specifies how response status code must be converted if matched. Converting only happens for error cases. This method can be called multiple
   * times and will chain conversion.
   */
  public ApplyResultOperationSelector<T> convertAndProxy(Status source, Status target) {
    return convertAndProxy(source.getStatusCode(), target.getStatusCode());
  }

  /**
   * Uses {@link Status#INTERNAL_SERVER_ERROR} status code.
   */
  public ApplyResultOperation<T> THROW_INTERNAL_SERVER_ERROR() {
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
  public ApplyResultOperation<T> THROW_BAD_GATEWAY() {
    return new ApplyResultOperation<>(
        resultWithStatus,
        of(MoreErrors.BAD_GATEWAY),
        ofNullable(proxiedStatusCodes),
        ofNullable(statusCodesConverter),
        errorMessage,
        predicates);
  }

  /**
   * Uses {@link Status#FORBIDDEN} status code.
   */
  public ApplyResultOperation<T> THROW_FORBIDDEN() {
    return new ApplyResultOperation<>(
        resultWithStatus,
        of(FORBIDDEN.getStatusCode()),
        ofNullable(proxiedStatusCodes),
        ofNullable(statusCodesConverter),
        errorMessage,
        predicates);
  }

  /**
   * Uses status code from {@link ResultWithStatus#getStatusCode()} (response code from remote call). Note that {@link Status#SERVICE_UNAVAILABLE}
   * will be replaced with {@link Status#BAD_GATEWAY}.
   */
  public ApplyResultOperation<T> PROXY_STATUS_CODE() {
    return new ApplyResultOperation<>(resultWithStatus, empty(), empty(), empty(), errorMessage, predicates);
  }

  /**
   * Sets default value to return.
   *
   * @param value
   *          default value to return in case of error
   */
  public ApplyResultOperation<T> RETURN_DEFAULT(T value) {
    return new ApplyResultOperation<>(resultWithStatus, empty(), empty(), empty(), errorMessage, predicates, of(value));
  }

  /**
   * Sets default value to return.
   *
   * @param value
   *          default value to return in case of error
   */
  public ApplyResultOperation<T> RETURN_DEFAULT(Supplier<T> value) {
    return new ApplyResultOperation<>(resultWithStatus, empty(), empty(), empty(), errorMessage, predicates, of(value.get()));
  }

}