package ru.hh.jclient.errors.impl.check;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.ws.rs.core.Response.Status;
import ru.hh.jclient.common.EmptyWithStatus;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.impl.PredicateWithStatus;
import static java.util.Optional.empty;

public class ApplyEmptyResultOperationSelector
    extends AbstractApplyResultOperationSelector<Void, ApplyEmptyResultOperationSelector, ApplyEmptyResultOperation> {

  public ApplyEmptyResultOperationSelector(EmptyWithStatus emptyWithStatus, String errorMessage, Object... params) {
    super(emptyWithStatus, errorMessage, params);
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
    return new ApplyEmptyResultOperation(resultWithStatus, empty(), empty(), empty(), errorMessage, predicates, empty());
  }

  @Override
  protected ApplyEmptyResultOperation createApplyOperation(ResultWithStatus<Void> wrapper,
      Optional<Integer> errorStatusCode,
      Optional<List<Integer>> proxiedStatusCodes,
      Optional<Function<Integer, Integer>> statusCodesConverter,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<Void>> predicates) {
    return new ApplyEmptyResultOperation(wrapper, errorStatusCode, proxiedStatusCodes, statusCodesConverter, errorMessage, predicates);
  }
}
