package ru.hh.jclient.common.check;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import static java.util.Optional.ofNullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.HttpClientEventListener;
import ru.hh.jclient.common.HttpHeaderNames;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.Uri;

public class GlobalTimeoutCheck implements HttpClientEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTimeoutCheck.class);

  private final Duration threshold;
  private final Function<Uri, String> uriCompactionFunction;
  private final ConcurrentMap<LoggingData, TimeoutCounter> timeoutStatistics;

  /**
   * Create check instance
   * @param threshold ignore diff between externalTimeout and current request if it is less than threshold
   * @param executorService executor to execut logging task on
   * @param intervalMs logging interval
   */
  public GlobalTimeoutCheck(Duration threshold, ScheduledExecutorService executorService, long intervalMs) {
    this.threshold = threshold;
    this.timeoutStatistics = new ConcurrentHashMap<>();
    this.uriCompactionFunction = (uri) -> ofNullable(uri).map(Uri::getPath).orElse(null);
    executorService.scheduleAtFixedRate(() -> logTimeouts(intervalMs), 0, intervalMs, TimeUnit.MILLISECONDS);
  }

  /**
   * Create check instance
   * @param threshold ignore diff between externalTimeout and current request if it is less than threshold
   * @param executorService executor to execut logging task on
   * @param uriCompactionFunction function to compact same urls with variable parts
   * @param intervalMs logging interval
   */
  public GlobalTimeoutCheck(
      Duration threshold,
      ScheduledExecutorService executorService,
      Function<Uri, String> uriCompactionFunction,
      long intervalMs
  ) {
    this.threshold = threshold;
    this.timeoutStatistics = new ConcurrentHashMap<>();
    this.uriCompactionFunction = uriCompactionFunction;
    executorService.scheduleAtFixedRate(() -> logTimeouts(intervalMs), 0, intervalMs, TimeUnit.MILLISECONDS);
  }

  protected void logTimeouts(long intervalMs) {
    var copy = Map.copyOf(timeoutStatistics);
    timeoutStatistics.clear();
    copy.forEach((data, timeoutCounter) -> {
      long currentRequestCount = timeoutCounter.longAdder.sum();
      long maxAlreadySpentMs = timeoutCounter.maxSpent.get();

      LOGGER.warn(
          "For last {} ms, got {} requests from <{}> expecting timeout={} ms, "
              + "but calling <{}> with timeout {} ms. "
              + "Arbitrary we spend up to {} ms before the call",
          intervalMs,
          currentRequestCount,
          data.userAgent,
          data.outerTimeout.toMillis(),
          data.uri,
          data.requestTimeout.toMillis(),
          maxAlreadySpentMs

      );
    });
  }

  @Override
  public void beforeExecute(HttpClient httpClient, Request request) {
    ofNullable(httpClient.getContext().getHeaders())
        .map(headers -> headers.get(HttpHeaderNames.X_OUTER_TIMEOUT_MS))
        .flatMap(values -> values.stream().findFirst())
        .map(Long::valueOf)
        .map(Duration::ofMillis)
        .ifPresent(outerTimeout -> {
          var alreadySpentTime = Duration.between(httpClient.getContext().getRequestStart(), getNow());
          var expectedTimeout = outerTimeout.minus(alreadySpentTime);
          var requestTimeout = Duration.ofMillis(request.getRequestTimeout());
          var diff = requestTimeout.minus(expectedTimeout);
          if (diff.compareTo(threshold) > 0) {
            var userAgent = ofNullable(httpClient.getContext().getHeaders())
                .map(headers -> headers.get(HttpHeaderNames.USER_AGENT))
                .flatMap(values -> values.stream().findFirst())
                .orElse("unknown");
            handleTimeoutExceeded(userAgent, request, outerTimeout, alreadySpentTime, requestTimeout);
          }
        });
  }

  protected LocalDateTime getNow() {
    return LocalDateTime.now();
  }

  protected void handleTimeoutExceeded(
      String userAgent,
      Request request,
      Duration outerTimeout,
      Duration alreadySpentTime,
      Duration requestTimeout
  ) {
    var data = new LoggingData(userAgent, uriCompactionFunction.apply(request.getUri()), requestTimeout, outerTimeout);
    timeoutStatistics.computeIfAbsent(data, key -> new TimeoutCounter()).increment(alreadySpentTime.toMillis());
  }

  private static final class LoggingData {
    private final String userAgent;
    private final String uri;
    private final Duration requestTimeout;
    private final Duration outerTimeout;

    private LoggingData(String userAgent, String uri, Duration requestTimeout, Duration outerTimeout) {
      this.userAgent = userAgent;
      this.uri = uri;
      this.requestTimeout = requestTimeout;
      this.outerTimeout = outerTimeout;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LoggingData that = (LoggingData) o;
      return Objects.equals(userAgent, that.userAgent) &&
          Objects.equals(uri, that.uri) &&
          requestTimeout.equals(that.requestTimeout) &&
          outerTimeout.equals(that.outerTimeout);
    }

    @Override
    public int hashCode() {
      return Objects.hash(userAgent, uri, requestTimeout, outerTimeout);
    }
  }

  private static final class TimeoutCounter {
    final LongAdder longAdder;
    final LongAccumulator maxSpent;

    TimeoutCounter() {
      longAdder = new LongAdder();
      maxSpent = new LongAccumulator(Long::max, 0);
    }

    public void increment(long alreadySpentMs) {
      longAdder.increment();
      maxSpent.accumulate(alreadySpentMs);
    }
  }
}
