package ru.hh.jclient.common.balancing;

import static ru.hh.jclient.common.HttpStatuses.CONNECT_TIMEOUT_ERROR;
import static ru.hh.jclient.common.HttpStatuses.SERVICE_UNAVAILABLE;
import ru.hh.jclient.common.Response;
import static ru.hh.jclient.common.ResponseStatusMessages.CONNECT_ERROR_MESSAGE;

import java.util.HashMap;
import java.util.Map;


public final class RetryPolicy {
  private Map<Integer, Boolean> rules = new HashMap<>();

   public RetryPolicy() {
    rules.put(CONNECT_TIMEOUT_ERROR, false);
    rules.put(SERVICE_UNAVAILABLE, false);
  }

  void update(Map<Integer, Boolean> config) {
    if (config != null && !config.isEmpty()) {
      rules = config;
    }
  }

  public boolean isRetriable(Response response, boolean idempotent) {
    int statusCode = response.getStatusCode();

    if (statusCode == CONNECT_TIMEOUT_ERROR && CONNECT_ERROR_MESSAGE.equals(response.getStatusText())) {
      return true;
    }

    Boolean retryNonIdempotent = rules.get(statusCode);
    if (retryNonIdempotent == null) {
      return false;
    }

    return retryNonIdempotent || idempotent;
  }

  public boolean isServerError(Response response) {
    int statusCode = response.getStatusCode();

    if (statusCode == CONNECT_TIMEOUT_ERROR && CONNECT_ERROR_MESSAGE.equals(response.getStatusText())) {
      return true;
    }

    return rules.containsKey(statusCode);
  }

  public Map<Integer, Boolean> getRules() {
    return Map.copyOf(this.rules);
  }

  @Override
  public String toString() {
    return "RetryPolicy {" + rules + '}';
  }
}
