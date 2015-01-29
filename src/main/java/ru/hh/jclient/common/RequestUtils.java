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

  public static boolean isInDebugMode(Map<String, List<String>> headers) {
    return "true".equalsIgnoreCase(getSingleHeader(headers, X_HH_DEBUG).orElse("false"));
  }

  public static Optional<String> getRequestId(Map<String, List<String>> headers) {
    return getSingleHeader(headers, X_REQUEST_ID);
  }

  public static Optional<String> getSingleHeader(Map<String, List<String>> headers, String headerName) {
    return getSingleHeader(headers, headerName, false);
  }

  public static Optional<String> getSingleHeader(Map<String, List<String>> headers, String headerName, boolean warnIfMultiple) {
    requireNonNull(headers, "headers must not be null");
    requireNonNull(headerName, "headerName must not be null");
    if (!headers.containsKey(headerName)) {
      return Optional.empty();
    }
    List<String> values = headers.get(headerName);
    if (values.size() > 1 && warnIfMultiple) {
      log.warn("Unexpected multiple values for header {}: {}", headerName, Joiner.on(',').useForNull("null").join(values));
    }
    return Optional.ofNullable(values.get(0));
  }
}
