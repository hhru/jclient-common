package ru.hh.jclient.common.check;

import org.junit.Test;
import ru.hh.jclient.common.HttpClientTestBase;
import ru.hh.jclient.common.HttpHeaderNames;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.Uri;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.net.MediaType.ANY_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static ru.hh.jclient.common.check.GlobalTimeoutCheck.REPLACEMENT;

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
    withContext(Map.of(HttpHeaderNames.X_OUTER_TIMEOUT_MS, List.of("100")))
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
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").setRequestTimeout(250).build();
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
            return now.plusSeconds(1);
          }
        })
        .okRequest(new byte[0], ANY_TYPE);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").setRequestTimeout(50).build();
    http.with(request).expectEmpty().result().get();
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
    http.with(request).expectEmpty().result().get();
  }

  @Test
  public void testCompactionWorksForNumbersAndHexHashes() {
    Uri uri = Uri.create("http://localhost:2800/resource/123456/daba9e610001f70104003acc866d55656d6a5a/get");
    assertEquals(
        new StringJoiner("/", "/", "").add("resource").add(REPLACEMENT).add(REPLACEMENT).add("get").toString(),
        GlobalTimeoutCheck.compactUrl(uri, 4, 16)
    );
  }

  @Test
  public void testCompactionDoesNotWorkForShortNumbersAndNonHexHashes() {
    String expected = "/resource/123/daka9e610001f70104003acc866d55656d6a5a/get";
    Uri uri = Uri.create("http://localhost:2800" + expected);
    assertEquals(expected, GlobalTimeoutCheck.compactUrl(uri, 4, 16));
  }
}
