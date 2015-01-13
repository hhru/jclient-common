package ru.hh.jclient.common;

import static ru.hh.jclient.common.HttpClient.HEADER_DEBUG;
import java.util.List;
import java.util.Map;

public class RequestUtils {

  public static boolean isInDebugMode(Map<String, List<String>> headers) {
    return headers.containsKey(HEADER_DEBUG) && headers.get(HEADER_DEBUG).size() == 1
        && "true".equals(headers.get(HEADER_DEBUG).get(0).toLowerCase());
  }

}
