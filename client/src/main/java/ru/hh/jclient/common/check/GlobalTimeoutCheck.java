package ru.hh.jclient.common.check;

import com.google.common.net.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.HttpClientEventListener;
import ru.hh.jclient.common.HttpHeaderNames;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.Uri;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

public class GlobalTimeoutCheck implements HttpClientEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalTimeoutCheck.class);
  private static final Duration DEFAULT_THRESHOLD = Duration.ofMillis(100);

  private final Duration threshold;
  @Nullable
  private final ConcurrentMap<WeakReference<LoggingData>, Integer> timeoutCounter;

  /**
   * per request logging may cause logging system overflow, so
   * use {@link GlobalTimeoutCheck#GlobalTimeoutCheck(java.time.Duration, java.util.concurrent.ScheduledExecutorService, long)}
   * which will aggregate info based on request parameters
   */
  @Deprecated
  public GlobalTimeoutCheck() {
    threshold = DEFAULT_THRESHOLD;
    this.timeoutCounter = null;
  }

  /**
   * Create check instance
   * @param threshold ignore diff between externalTimeout and current request if it is less than threshold
   * @param executorService executor to execut logging task on
   * @param intervalMs logging interval
   */
  public GlobalTimeoutCheck(Duration threshold, ScheduledExecutorService executorService, long intervalMs) {
    this.threshold = threshold;
    this.timeoutCounter = new ConcurrentHashMap<>();
    executorService.scheduleAtFixedRate(() -> logTimeouts(intervalMs), 0, intervalMs, TimeUnit.MILLISECONDS);
  }

  protected void logTimeouts(long intervalMs) {
    timeoutCounter.replaceAll((dataReference, count) -> {
      if (dataReference == null || count == null || count == 0) {
        return null;
      }
      ofNullable(dataReference.get()).ifPresent(data -> {
        if (count == 1) {
          logSingleRequest(data);
        }
        LOGGER.error("For last {} ms, got {} requests from <{}> expecting timeout={} ms, "
                     + "but calling <{}> with timeout {} ms",
          intervalMs,
          count,
          data.userAgent,
          data.outerTimeout.toMillis(),
          data.uri,
          data.requestTimeout.toMillis()
        );
      });
      return null;
    });
  }

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
        var diff = requestTimeout.minus(expectedTimeout);
        if (diff.compareTo(threshold) > 0) {
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
    var data = new LoggingData(userAgent, ofNullable(request.getUri()).map(Uri::getPath).orElse(null),
                               requestTimeout, outerTimeout, alreadySpentTime);
    if (timeoutCounter == null) {
      logSingleRequest(data);
      return;
    }
    timeoutCounter.compute(new WeakReference<>(data), (currentData, currentCount) -> {
      if (currentCount == null) {
        return  1;
      }
      return currentCount + 1;
    });
  }

  private static void logSingleRequest(LoggingData data) {
    LOGGER.error("Incoming request from <{}> expects timeout={} ms, we have been working already for {} ms "
            + "and now trying to call <{}> with timeout {} ms",
        data.userAgent,
        data.outerTimeout.toMillis(),
        data.alreadySpentTime.toMillis(),
        data.uri,
        data.requestTimeout.toMillis()
    );
  }

  private static final class LoggingData {
    private final String userAgent;
    private final String uri;
    private final Duration requestTimeout;
    private final Duration outerTimeout;
    private final Duration alreadySpentTime;

    private LoggingData(String userAgent, String uri, Duration requestTimeout, Duration outerTimeout, Duration alreadySpentTime) {
      this.userAgent = userAgent;
      this.uri = uri;
      this.requestTimeout = requestTimeout;
      this.outerTimeout = outerTimeout;
      this.alreadySpentTime = alreadySpentTime;
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
}
