package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;
import static ru.hh.jclient.common.HttpHeaders.X_HH_DEBUG;
import static ru.hh.jclient.common.HttpHeaders.X_REQUEST_ID;
import com.google.common.base.Joiner;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestUtils {

  private static final Logger log = LoggerFactory.getLogger(RequestUtils.class);

  /**
   * Check if headers contain {@link HttpHeaders#X_HH_DEBUG} with value of 'true'.
   *
   * @param headers
   *          ideally, case-insensitive map, otherwise keys must be lower-case, or header name must be equal to the key in map
   */
  public static boolean isInDebugMode(Map<String, List<String>> headers) {
    return "true".equalsIgnoreCase(getSingleHeader(headers, X_HH_DEBUG).orElse("false"));
  }

  /**
   * See {@link #getSingleHeader(Map, String, boolean)} for {@link HttpHeaders#X_REQUEST_ID} with warning off.
   */
  public static Optional<String> getRequestId(Map<String, List<String>> headers) {
    return getSingleHeader(headers, X_REQUEST_ID);
  }

  /**
   * See {@link #getSingleHeader(Map, String, boolean)} with warning off
   */
  public static Optional<String> getSingleHeader(Map<String, List<String>> headers, String headerName) {
    return getSingleHeader(headers, headerName, false);
  }

  /**
   * Get value (first, if multiple) for specified header.
   *
   * @param headers
   *          ideally, case-insensitive map, otherwise keys must be lower-case, or header name must be equal to the key in map
   * @param headerName
   *          name of header
   * @param warnIfMultiple
   *          log warning if there are multiple values for the header
   * @return Optional containing value or empty if no value found
   */
  public static Optional<String> getSingleHeader(Map<String, List<String>> headers, String headerName, boolean warnIfMultiple) {
    requireNonNull(headers, "headers must not be null");
    requireNonNull(headerName, "headerName must not be null");

    if (headers.containsKey(headerName)) {
      return getWhenExists(headers, headerName, warnIfMultiple);
    }

    // for cases when headers map is not case-insensitive but was converted from map where all keys are lower-cased
    headerName = headerName.toLowerCase();

    if (headers.containsKey(headerName)) {
      return getWhenExists(headers, headerName, warnIfMultiple);
    }

    return Optional.empty();
  }

  private static Optional<String> getWhenExists(Map<String, List<String>> headers, String headerName, boolean warnIfMultiple) {
    List<String> values = headers.get(headerName);
    if (values.size() > 1 && warnIfMultiple) {
      log.warn("Unexpected multiple values for header {}: {}", headerName, Joiner.on(',').useForNull("null").join(values));
    }
    return Optional.ofNullable(values.get(0));

  }
}
