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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.net.MediaType.ANY_TYPE;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GlobalTimeoutCheckTest extends HttpClientTestBase {

  @Test
  public void testShouldNotTriggerIfTimeoutsOk() throws ExecutionException, InterruptedException {
    LocalDateTime now = LocalDateTime.now();
    withContext(Map.of(HttpHeaderNames.X_OUTER_TIMEOUT_MS, List.of("100")))
      .withEventListener(new GlobalTimeoutCheck() {
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
    http.with(request).expectEmpty().result().get();
  }

  @Test
  public void testShouldTriggerIfTimeoutsNotOk() throws ExecutionException, InterruptedException {
    LocalDateTime now = LocalDateTime.now();
    AtomicBoolean triggered = new AtomicBoolean(false);
    withContext(Map.of(HttpHeaderNames.X_OUTER_TIMEOUT_MS, List.of("50")))
        .withEventListener(new GlobalTimeoutCheck() {
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
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").setRequestTimeout(100).build();
    http.with(request).expectEmpty().result().get();
    assertTrue(GlobalTimeoutCheck.class + " not triggered", triggered.get());
  }

  @Test
  public void testShouldTriggerIfTimeoutsOkButTooLongSinceStart() throws ExecutionException, InterruptedException {
    LocalDateTime now = LocalDateTime.now();
    AtomicBoolean triggered = new AtomicBoolean(false);
    withContext(Map.of(HttpHeaderNames.X_OUTER_TIMEOUT_MS, List.of("100")))
        .withEventListener(new GlobalTimeoutCheck() {
          @Override
          protected void handleTimeoutExceeded(String userAgent, Request request, Duration outerTimeout,
                                               Duration alreadySpentTime, Duration requestTimeout) {
            triggered.set(true);
          }

          @Override
          protected LocalDateTime getNow() {
            return now.plusNanos(TimeUnit.MILLISECONDS.toNanos(100));
          }
        })
        .okRequest(new byte[0], ANY_TYPE);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").setRequestTimeout(50).build();
    http.with(request).expectEmpty().result().get();
    assertTrue(GlobalTimeoutCheck.class + " not triggered", triggered.get());
  }


}
