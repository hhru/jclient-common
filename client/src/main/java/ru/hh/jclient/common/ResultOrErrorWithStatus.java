package ru.hh.jclient.common;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Wrapper object that contains response status code and either normal or ERROR result of conversion. This wrapper can be used outside of implementing
 * client.
 * 
 * @param <T> type of conversion result
 * @param <E> type of ERROR conversion result
 */
public class ResultOrErrorWithStatus<T, E> extends ResultWithStatus<T> {

  @Nullable
  private final E errorValue;

  public ResultOrErrorWithStatus(@Nullable T value, @Nullable E errorValue, int statusCode) {
    super(value, statusCode);
    this.errorValue = errorValue;
  }

  /**
   * @return result of ERROR response conversion. {@link Optional#empty() Empty} if error did not happen
   */
  public Optional<E> getError() {
    return Optional.ofNullable(errorValue);
  }

  /**
   * @return true if error value is not present
   */
  @Override
  public boolean isSuccess() {
    return super.isSuccess() && errorValue == null;
  }
}
