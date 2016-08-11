package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;
import java.util.Optional;

/**
 * Wrapper object that contains response status code and either normal or ERROR result of conversion. This wrapper can be used outside of implementing
 * client.
 * 
 * @param <T> type of conversion result
 * @param <E> type of ERROR conversion result
 */
public class ResultOrErrorWithStatus<T, E> extends ResultWithStatus<T> {

  private Optional<E> errorValue;

  public ResultOrErrorWithStatus(Optional<T> value, Optional<E> errorValue, int statusCode) {
    super(value.orElse(null), statusCode);
    this.errorValue = requireNonNull(errorValue, "errorValue must not be null");
  }

  /**
   * @return result of ERROR response conversion. {@link Optional#empty() Empty} if error did not happen
   */
  public Optional<E> getError() {
    return errorValue;
  }

  /**
   * @return true if error value is not present
   */
  @Override
  public boolean isSuccess() {
    return super.isSuccess() && !errorValue.isPresent();
  }
}
