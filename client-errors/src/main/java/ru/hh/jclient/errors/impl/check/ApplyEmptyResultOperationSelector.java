package ru.hh.jclient.errors.impl.check;

import javax.ws.rs.core.Response.Status;
import ru.hh.jclient.common.EmptyWithStatus;
import static java.util.Optional.empty;

public class ApplyEmptyResultOperationSelector extends AbstractApplyResultOperationSelector<Void, ApplyEmptyResultOperationSelector> {

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
  public ApplyResultOperation<Void> returnEmpty() {
    return new ApplyResultOperation<>(resultWithStatus, empty(), empty(), empty(), errorMessage, predicates, empty());
  }

}
