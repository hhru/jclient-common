package ru.hh.jclient.common;

import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static ru.hh.jclient.common.HttpHeaderNames.X_HH_DEBUG;
import static ru.hh.jclient.common.HttpHeaderNames.X_REQUEST_ID;

public class RequestUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestUtils.class);

  /**
   * Check if headers contain {@link HttpHeaderNames#X_HH_DEBUG} with value of 'true' or query params contain {@link HttpParams#DEBUG} param with any
   * value.
   *
   * @param headers
   *          ideally, case-insensitive map, otherwise keys must be lower-case, or header name must be equal to the key in map
   * @param queryParams
   *          map of query params
   */
  public static boolean isInDebugMode(Map<String, List<String>> headers, Map<String, List<String>> queryParams) {
    boolean hasDebugHeader = "true".equalsIgnoreCase(getSingleHeader(headers, X_HH_DEBUG).orElse("false"));
    boolean hasDebugParam = queryParams.containsKey(HttpParams.DEBUG);
    return hasDebugHeader || hasDebugParam;
  }

  /**
   * See {@link #getSingleHeader(Map, String, boolean)} for {@link HttpHeaderNames#X_REQUEST_ID} with warning off.
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
   * Get a long value from the specified header.
   * 
   * @param headers ideally, case-insensitive map, otherwise keys must be lower-case, or header name must be equal to the key in map
   * @param headerName name of the header to extract the long value from
   * @return Optional containing the parsed long value, or empty if the header is not present or cannot be parsed as a long
   */
  public static Optional<Long> getLongHeaderValue(Map<String, List<String>> headers, String headerName) {
    return getSingleHeader(headers, headerName).map(l -> RequestUtils.parseLongValue(l, headerName));
  }

  /**
   * Parse a string value as a long number.
   * 
   * @param value the string value to parse
   * @param headerName the name of the header (used for logging purposes)
   * @return the parsed long value, or null if the value cannot be parsed as a long
   */
  private static Long parseLongValue(String value, String headerName) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      LOGGER.debug("Failed to parse long from header {} with value '{}'", headerName, value);
      return null;
    }
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
      LOGGER.warn(
          "Unexpected multiple values for header {}: {}",
          headerName,
          values.stream().map(v -> v == null ? "null" : v).collect(Collectors.joining(",")));
    }
    return Optional.ofNullable(values.get(0));

  }
}
