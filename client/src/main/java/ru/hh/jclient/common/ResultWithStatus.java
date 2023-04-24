package ru.hh.jclient.common;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Wrapper object that contains response status code and result of conversion. This wrapper can be used outside of implementing client.
 * 
 * @param <T> type of conversion result
 */
public class ResultWithStatus<T> {

  @Nullable
  private T value;
  private int statusCode;

  public ResultWithStatus(@Nullable T value, int statusCode) {
    this.value = value;
    this.statusCode = statusCode;
  }

  /**
   * @return result of response conversion. Can be {@link Optional#empty() empty} if error has happened
   */
  public Optional<T> get() {
    return Optional.ofNullable(value);
  }

  /**
   * @return response status code
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * @return true if response status code is within {@link HttpClient#OK_RANGE}
   */
  public boolean isSuccess() {
    return HttpClient.OK_RANGE.contains(statusCode);
  }
}
