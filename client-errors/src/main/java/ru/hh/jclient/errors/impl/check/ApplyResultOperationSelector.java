package ru.hh.jclient.errors.impl.check;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.ws.rs.core.Response.Status;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.impl.PredicateWithStatus;
import static java.util.Optional.empty;
import static java.util.Optional.of;

public class ApplyResultOperationSelector<T>
    extends AbstractApplyResultOperationSelector<T, ApplyResultOperationSelector<T>, ApplyResultOperation<T>> {

  public ApplyResultOperationSelector(ResultWithStatus<T> resultWithStatus, String errorMessage, Object... params) {
    super(resultWithStatus, errorMessage, params);
  }

  @Override
  protected ApplyResultOperation<T> createApplyOperation(ResultWithStatus<T> wrapper,
      Optional<Integer> errorStatusCode,
      Optional<List<Integer>> proxiedStatusCodes,
      Optional<Function<Integer, Integer>> statusCodesConverter,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<T>> predicates) {
    return new ApplyResultOperation<>(wrapper, errorStatusCode, proxiedStatusCodes, statusCodesConverter, errorMessage, predicates);
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
