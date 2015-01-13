package ru.hh.jclient.common;

import static ru.hh.jclient.common.HttpClient.HEADER_DEBUG;
import static ru.hh.jclient.common.HttpClient.HEADER_REQUEST_ID;

import com.google.common.base.Joiner;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestUtils {

  private static final Logger log = LoggerFactory.getLogger(RequestUtils.class);

  public static boolean isInDebugMode(Map<String, List<String>> headers) {
    return "true".equalsIgnoreCase(getSingleHeader(headers, HEADER_DEBUG, null));
  }

  public static String getRequestId(Map<String, List<String>> headers) {
    return getSingleHeader(headers, HEADER_REQUEST_ID);
  }

  public static String getSingleHeader(Map<String, List<String>> headers, String headerName) {
    return getSingleHeader(headers, headerName, null);
  }

  public static String getSingleHeader(Map<String, List<String>> headers, String headerName, String defaultValue) {
    return getSingleHeader(headers, headerName, defaultValue, true);
  }

  public static String getSingleHeader(Map<String, List<String>> headers, String headerName, String defaultValue, boolean warnIfMultiple) {
    if (!headers.containsKey(headerName)) {
      return defaultValue;
    }
    List<String> values = headers.get(headerName);
    if (values.size() > 1) {
      if (warnIfMultiple) {
        log.warn("Unexpected multiple values for header {}: {}", headerName, Joiner.on(',').useForNull("null").join(values));
      }
      return defaultValue;
    }
    return values.get(0);
  }
}
