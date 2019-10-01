package ru.hh.jclient.common.check;

import com.google.common.net.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.HttpClientEventListener;
import ru.hh.jclient.common.HttpHeaderNames;
import ru.hh.jclient.common.Request;

import java.time.Duration;
import java.time.LocalDateTime;

import static java.util.Optional.ofNullable;

public class GlobalTimeoutCheck implements HttpClientEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTimeoutCheck.class);
  @Override
  public void beforeExecute(HttpClient httpClient, Request request) {
    ofNullable(httpClient.getContext().getHeaders())
      .map(headers -> headers.get(HttpHeaderNames.X_OUTER_TIMEOUT_MS))
      .flatMap(values -> values.stream().findFirst())
      .map(Long::valueOf).map(Duration::ofMillis)
      .ifPresent(outerTimeout -> {
        var alreadySpentTime = Duration.between(httpClient.getContext().getRequestStart(), getNow());
        var expectedTimeout = outerTimeout.minus(alreadySpentTime);
        var requestTimeout = Duration.ofMillis(request.getRequestTimeout());
        if (requestTimeout.compareTo(expectedTimeout) > 0) {
          var userAgent = ofNullable(httpClient.getContext().getHeaders())
              .map(headers -> headers.get(HttpHeaders.USER_AGENT))
              .flatMap(values -> values.stream().findFirst())
              .orElse("unknown");
          handleTimeoutExceeded(userAgent, request, outerTimeout, alreadySpentTime, requestTimeout);
        }
      });
  }

  protected LocalDateTime getNow() {
    return LocalDateTime.now();
  }

  protected void handleTimeoutExceeded(String userAgent, Request request, Duration outerTimeout,
                                     Duration alreadySpentTime, Duration requestTimeout) {
    LOGGER.error("Incoming request from <{}> expects timeout={} ms, we have been working already for {} ms " +
      "and now trying to call <{}> with timeout {} ms",
      userAgent,
      outerTimeout.toMillis(),
      alreadySpentTime.toMillis(),
      request.getUri().getPath(),
      requestTimeout.toMillis()
    );
  }
}
