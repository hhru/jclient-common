package ru.hh.jclient.common.balancing;

import static ru.hh.jclient.common.AbstractClient.HTTP_POST;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_CONNECT_ERROR;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_REQUEST_TIMEOUT;

public class RetryPolicy {
  private boolean connectTimeout = true;
  private boolean requestTimeout = true;
  private boolean nonIdempotentRequestTimeout = false;

  void update(String configString) {
    connectTimeout = configString.contains("timeout");
    requestTimeout = configString.contains("http_503");
    nonIdempotentRequestTimeout = configString.contains("non_idempotent_503");
  }

  boolean isRetriable(int statusCode, String method) {
    if (nonIdempotentRequestTimeout && !isIdempotent(method) && statusCode == STATUS_REQUEST_TIMEOUT) {
      return true;
    }
    if (requestTimeout && isIdempotent(method) && statusCode == STATUS_REQUEST_TIMEOUT) {
      return true;
    }
    return connectTimeout && statusCode == STATUS_CONNECT_ERROR;
  }

  private static boolean isIdempotent(String method) {
    return ! HTTP_POST.equals(method);
  }

  boolean isConnectTimeout() {
    return connectTimeout;
  }

  boolean isRequestTimeout() {
    return requestTimeout;
  }

  boolean isNonIdempotentRequestTimeout() {
    return nonIdempotentRequestTimeout;
  }
}
