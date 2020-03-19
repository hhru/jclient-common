package ru.hh.jclient.common.check;

import org.junit.Test;
import ru.hh.jclient.common.HttpClientTestBase;
import ru.hh.jclient.common.HttpHeaderNames;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.net.MediaType.ANY_TYPE;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class GlobalTimeoutCheckTest extends HttpClientTestBase {

  private static final Duration DEFAULT_DURATION = Duration.ofMillis(100);

  @Test
  public void testShouldNotTriggerIfTimeoutsOk() throws ExecutionException, InterruptedException {
    LocalDateTime now = LocalDateTime.now();
    withContext(Map.of(HttpHeaderNames.X_OUTER_TIMEOUT_MS, List.of("100")))
      .withEventListener(new GlobalTimeoutCheck(DEFAULT_DURATION, mock(ScheduledExecutorService.class), 0) {
        @Override
        protected void handleTimeoutExceeded(String userAgent, Request request, Duration outerTimeout,
                                             Duration alreadySpentTime, Duration requestTimeout) {
          fail(GlobalTimeoutCheck.class + " false triggered");
        }

        @Override
        protected LocalDateTime getNow() {
          return now;
        }
      })
      .okRequest(new byte[0], ANY_TYPE);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").setRequestTimeout(50).build();
    http.with(request).expectNoContent().result().get();
  }

  @Test
  public void testShouldTriggerIfTimeoutsNotOk() throws ExecutionException, InterruptedException {
    LocalDateTime now = LocalDateTime.now();
    AtomicBoolean triggered = new AtomicBoolean(false);
    withContext(Map.of(HttpHeaderNames.X_OUTER_TIMEOUT_MS, List.of("100")))
        .withEventListener(new GlobalTimeoutCheck(DEFAULT_DURATION, mock(ScheduledExecutorService.class), 0) {
          @Override
          protected void handleTimeoutExceeded(String userAgent, Request request, Duration outerTimeout,
                                               Duration alreadySpentTime, Duration requestTimeout) {
            triggered.set(true);
          }

          @Override
          protected LocalDateTime getNow() {
            return now;
          }
        })
        .okRequest(new byte[0], ANY_TYPE);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").setRequestTimeout(250).build();
    http.with(request).expectNoContent().result().get();
    assertTrue(GlobalTimeoutCheck.class + " not triggered", triggered.get());
  }

  @Test
  public void testShouldTriggerIfTimeoutsOkButTooLongSinceStart() throws ExecutionException, InterruptedException {
    LocalDateTime now = LocalDateTime.now();
    AtomicBoolean triggered = new AtomicBoolean(false);
    withContext(Map.of(HttpHeaderNames.X_OUTER_TIMEOUT_MS, List.of("100")))
        .withEventListener(new GlobalTimeoutCheck(DEFAULT_DURATION, mock(ScheduledExecutorService.class), 0) {
          @Override
          protected void handleTimeoutExceeded(String userAgent, Request request, Duration outerTimeout,
                                               Duration alreadySpentTime, Duration requestTimeout) {
            triggered.set(true);
          }

          @Override
          protected LocalDateTime getNow() {
            return now.plusSeconds(1);
          }
        })
        .okRequest(new byte[0], ANY_TYPE);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").setRequestTimeout(50).build();
    http.with(request).expectNoContent().result().get();
    assertTrue(GlobalTimeoutCheck.class + " not triggered", triggered.get());
  }

  @Test
  public void testShouldNotTriggerIfInThreshold() throws ExecutionException, InterruptedException {
    LocalDateTime now = LocalDateTime.now();
    withContext(Map.of(HttpHeaderNames.X_OUTER_TIMEOUT_MS, List.of("100")))
        .withEventListener(new GlobalTimeoutCheck(Duration.ofMillis(30), mock(ScheduledExecutorService.class), 0) {
          @Override
          protected void handleTimeoutExceeded(String userAgent, Request request, Duration outerTimeout,
                                               Duration alreadySpentTime, Duration requestTimeout) {
            fail(GlobalTimeoutCheck.class + " false triggered");
          }

          @Override
          protected LocalDateTime getNow() {
            return now.plusNanos(TimeUnit.MILLISECONDS.toNanos(15));
          }
        })
        .okRequest(new byte[0], ANY_TYPE);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").setRequestTimeout(100).build();
    http.with(request).expectNoContent().result().get();
  }
}
