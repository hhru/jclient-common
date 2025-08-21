package ru.hh.jclient.common.listener;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import ru.hh.jclient.common.HttpClientTestBase;
import ru.hh.jclient.common.HttpHeaderNames;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import static ru.hh.jclient.common.util.ContentType.ANY;

public class GlobalTimeoutCheckTest extends HttpClientTestBase {

  private static final Duration DEFAULT_DURATION = Duration.ofMillis(100);

  @Test
  public void testShouldNotTriggerIfTimeoutsOk() throws ExecutionException, InterruptedException {
    OffsetDateTime now = OffsetDateTime.now();
    withContext(
        Map.of(HttpHeaderNames.X_OUTER_TIMEOUT_MS, List.of("100")),
        new GlobalTimeoutCheck(DEFAULT_DURATION, mock(ScheduledExecutorService.class), 0) {
          @Override
          protected void handleTimeoutExceeded(
              String userAgent,
              Request request,
              Duration outerTimeout,
              Duration alreadySpentTime,
              Duration requestTimeout
          ) {
            fail(GlobalTimeoutCheck.class + " false triggered");
          }

          @Override
          protected OffsetDateTime getNow() {
            return now;
          }
        }
    ).okRequest(new byte[0], ANY);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").setRequestTimeout(50).build();
    http.with(request).expectNoContent().result().get();
  }

  @Test
  public void testShouldTriggerIfTimeoutsNotOk() throws ExecutionException, InterruptedException {
    OffsetDateTime now = OffsetDateTime.now();
    AtomicBoolean triggered = new AtomicBoolean(false);
    withContext(
        Map.of(HttpHeaderNames.X_OUTER_TIMEOUT_MS, List.of("100")),
        new GlobalTimeoutCheck(DEFAULT_DURATION, mock(ScheduledExecutorService.class), 0) {
          @Override
          protected void handleTimeoutExceeded(
              String userAgent,
              Request request,
              Duration outerTimeout,
              Duration alreadySpentTime,
              Duration requestTimeout
          ) {
            triggered.set(true);
          }

          @Override
          protected OffsetDateTime getNow() {
            return now;
          }
        }
    ).okRequest(new byte[0], ANY);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").setRequestTimeout(250).build();
    http.with(request).expectNoContent().result().get();
    assertTrue(triggered.get(), () -> GlobalTimeoutCheck.class + " not triggered");
  }

  @Test
  public void testShouldTriggerIfTimeoutsOkButTooLongSinceStart() throws ExecutionException, InterruptedException {
    OffsetDateTime now = OffsetDateTime.now();
    AtomicBoolean triggered = new AtomicBoolean(false);
    withContext(
        Map.of(HttpHeaderNames.X_OUTER_TIMEOUT_MS, List.of("100")),
        new GlobalTimeoutCheck(DEFAULT_DURATION, mock(ScheduledExecutorService.class), 0) {
          @Override
          protected void handleTimeoutExceeded(
              String userAgent,
              Request request,
              Duration outerTimeout,
              Duration alreadySpentTime,
              Duration requestTimeout
          ) {
            triggered.set(true);
          }

          @Override
          protected OffsetDateTime getNow() {
            return now.plusSeconds(1);
          }
        }
    ).okRequest(new byte[0], ANY);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").setRequestTimeout(50).build();
    http.with(request).expectNoContent().result().get();
    assertTrue(triggered.get(), () -> GlobalTimeoutCheck.class + " not triggered");
  }

  @Test
  public void testShouldNotTriggerIfInThreshold() throws ExecutionException, InterruptedException {
    OffsetDateTime now = OffsetDateTime.now();
    withContext(
        Map.of(HttpHeaderNames.X_OUTER_TIMEOUT_MS, List.of("100")),
        new GlobalTimeoutCheck(Duration.ofMillis(30), mock(ScheduledExecutorService.class), 0) {
          @Override
          protected void handleTimeoutExceeded(
              String userAgent,
              Request request,
              Duration outerTimeout,
              Duration alreadySpentTime,
              Duration requestTimeout
          ) {
            fail(GlobalTimeoutCheck.class + " false triggered");
          }

          @Override
          protected OffsetDateTime getNow() {
            return now.plusNanos(TimeUnit.MILLISECONDS.toNanos(15));
          }
        }
    ).okRequest(new byte[0], ANY);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").setRequestTimeout(100).build();
    http.with(request).expectNoContent().result().get();
  }

  @Test
  public void testShouldNotTriggerIfNoOuterTimeoutHeader() throws ExecutionException, InterruptedException {
    OffsetDateTime now = OffsetDateTime.now();
    withContext(
        Map.of(), // Empty headers - no X_OUTER_TIMEOUT_MS
        new GlobalTimeoutCheck(DEFAULT_DURATION, mock(ScheduledExecutorService.class), 0) {
          @Override
          protected void handleTimeoutExceeded(
              String userAgent,
              Request request,
              Duration outerTimeout,
              Duration alreadySpentTime,
              Duration requestTimeout
          ) {
            fail(GlobalTimeoutCheck.class + " should not be triggered when X_OUTER_TIMEOUT_MS header is missing");
          }

          @Override
          protected OffsetDateTime getNow() {
            return now;
          }
        }
    ).okRequest(new byte[0], ANY);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").setRequestTimeout(250).build();
    http.with(request).expectNoContent().result().get();
    // Test passes if handleTimeoutExceeded is not called (no exception thrown)
  }
}
