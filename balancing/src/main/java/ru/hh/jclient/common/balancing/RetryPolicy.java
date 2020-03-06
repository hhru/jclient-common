package ru.hh.jclient.common.balancing;

import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResponseStatusMessages;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


final class RetryPolicy {
  private static final Pattern HTTP_RETRY = Pattern.compile("http_([0-9]{3})");
  private static final Pattern NON_IDEMPOTENT_RETRY = Pattern.compile("non_idempotent_([0-9]{3})");

  private Map<Integer, Boolean> rules = new HashMap<>();

  RetryPolicy() {
    rules.put(HttpStatuses.REQUEST_TIMEOUT, false);
    rules.put(HttpStatuses.SERVICE_UNAVAILABLE, false);
  }

  void update(String configString) {
    this.rules = Stream.of(configString.split(","))
      .map(configElement -> {
        if ("timeout".equals(configElement)) {
          return new CodeIdempotence(HttpStatuses.REQUEST_TIMEOUT, false);
        }

        Matcher httpRetry = HTTP_RETRY.matcher(configElement);
        if (httpRetry.matches()) {
          return new CodeIdempotence(Integer.parseInt(httpRetry.group(1)), false);
        }

        Matcher nonIdempotentRetry = NON_IDEMPOTENT_RETRY.matcher(configElement);
        if (nonIdempotentRetry.matches()) {
          return new CodeIdempotence(Integer.parseInt(nonIdempotentRetry.group(1)), true);
        }

        return null;
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toMap(ci -> ci.code, ci -> ci.idempotent, (i1, i2) -> i1 || i2));
  }

  boolean isRetriable(Response response, boolean idempotent) {
    int statusCode = response.getStatusCode();

    if (statusCode == HttpStatuses.CONNECT_ERROR && ResponseStatusMessages.CONNECT_ERROR_MESSAGE.equals(response.getStatusText())) {
      return true;
    }

    Boolean retryNonIdempotent = rules.get(statusCode);
    if (retryNonIdempotent == null) {
      return false;
    }

    return retryNonIdempotent || idempotent;
  }

  boolean isServerError(Response response) {
    int statusCode = response.getStatusCode();

    if (statusCode == HttpStatuses.CONNECT_ERROR && ResponseStatusMessages.CONNECT_ERROR_MESSAGE.equals(response.getStatusText())) {
      return true;
    }

    return rules.containsKey(statusCode);
  }

  Map<Integer, Boolean> getRules() {
    return Map.copyOf(this.rules);
  }

  @Override
  public String toString() {
    return "RetryPolicy {" + rules + '}';
  }

  private static final class CodeIdempotence {
    final int code;
    final boolean idempotent;

    CodeIdempotence(int code, boolean idempotent) {
      this.code = code;
      this.idempotent = idempotent;
    }
  }
}
