package ru.hh.jclient.common;

import java.time.OffsetDateTime;
import java.util.Collections;
import static java.util.Collections.singletonList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.hh.jclient.common.HttpHeaderNames.X_DEADLINE_TIMEOUT_MS;
import static ru.hh.jclient.common.HttpHeaderNames.X_OUTER_TIMEOUT_MS;
import ru.hh.jclient.common.listener.DeadlineCheckerAndPropagator;
import ru.hh.jclient.common.util.storage.StorageUtils;

public class DeadlineCheckerAndPropagatorTest {

  private DeadlineCheckerAndPropagator injector;
  private HttpClientContextThreadLocalSupplier contextSupplier;
  private HttpClient httpClient;

  @BeforeEach
  public void setUp() {
    contextSupplier = new HttpClientContextThreadLocalSupplier();
    injector = new DeadlineCheckerAndPropagator(contextSupplier);
    httpClient = mock(HttpClient.class);
  }

  @Test
  public void testChooseRequestTimeout() {
    // Setup context with deadline
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(X_DEADLINE_TIMEOUT_MS, singletonList("1000"));
    headers.put(X_OUTER_TIMEOUT_MS, singletonList("1"));
    int requestTimeout = 500;
    // Create request with timeout
    Request request = new RequestBuilder()
        .setUrl("http://localhost/test")
        .setRequestTimeout(requestTimeout)
        .build();

    RequestBuilder requestBuilder = new RequestBuilder(request);

    // Execute with context
    contextSupplier.forCurrentThread()
        .withHeaders(headers)
        .execute(() -> {
          injector.beforeExecute(httpClient, requestBuilder, request);

          // Verify header was injected with correct value
          assertEquals(String.valueOf(requestTimeout), request.getHeaders().get(X_DEADLINE_TIMEOUT_MS));
          assertEquals(String.valueOf(requestTimeout), request.getHeaders().get(X_OUTER_TIMEOUT_MS));
          Request requestBuilded = requestBuilder.build();
          assertEquals(requestTimeout, requestBuilded.getRequestTimeout());
          assertTrue(requestBuilded.isDeadlineEnabled());
        });
  }

  @Test
  public void testChooseDeadlineHeader() {
    // Setup context with deadline
    int requestTimeout = 200;
    int deadlineTimeout = 100;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(X_DEADLINE_TIMEOUT_MS, singletonList(String.valueOf(deadlineTimeout)));
    headers.put(X_OUTER_TIMEOUT_MS, singletonList("1"));

    // Create request with timeout
    Request request = new RequestBuilder()
        .setUrl("http://localhost/test")
        .setRequestTimeout(requestTimeout)
        .build();

    RequestBuilder requestBuilder = new RequestBuilder(request);

    // Execute with context
    contextSupplier.forCurrentThread()
        .withHeaders(headers)
        .execute(() -> {
          injector.beforeExecute(httpClient, requestBuilder, request);

          // Verify header was injected with correct value (min of deadline and request timeout)
          int header = Integer.parseInt(request.getHeaders().get(X_DEADLINE_TIMEOUT_MS));
          assertTrue(header > 10 && header <= deadlineTimeout);
          assertEquals(String.valueOf(requestTimeout), request.getHeaders().get(X_OUTER_TIMEOUT_MS));
          // Verify request timeout is adjusted to minimum of deadline and request timeout
          int requestTimeoutLeft = requestBuilder.build().getRequestTimeout();
          assertTrue(requestTimeoutLeft > 10 && requestTimeoutLeft <= deadlineTimeout);
        });
  }

  @Test
  public void testChooseOuterHeader() {
    // Setup context with deadline
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(X_OUTER_TIMEOUT_MS, singletonList("100"));
    int requestTimeout = 200;
    // Create request with timeout
    Request request = new RequestBuilder()
        .setUrl("http://localhost/test")
        .setRequestTimeout(requestTimeout)
        .build();

    RequestBuilder requestBuilder = new RequestBuilder(request);

    // Execute with context
    contextSupplier.forCurrentThread()
        .withHeaders(headers)
        .execute(() -> {
          injector.beforeExecute(httpClient, requestBuilder, request);

          // Verify header was injected with correct value (min of deadline and request timeout)
          int header = Integer.parseInt(request.getHeaders().get(X_DEADLINE_TIMEOUT_MS));
          assertTrue(header > 10 && header <= 100);
          // Verify request timeout is adjusted to minimum of deadline and request timeout
          assertEquals(String.valueOf(requestTimeout), request.getHeaders().get(X_OUTER_TIMEOUT_MS));
          int requestTimeoutLeft = requestBuilder.build().getRequestTimeout();
          assertTrue(requestTimeoutLeft > 10 && requestTimeoutLeft <= 100);
        });
  }

  @Test
  public void testNoDeadlineContext() {
    // Create request with timeout using RequestBuilder
    int requestTimeout = 233;

    Request request = new RequestBuilder()
        .setUrl("http://localhost/test")
        .setRequestTimeout(requestTimeout)
        .build();

    RequestBuilder requestBuilder = new RequestBuilder(request);

    // Execute without deadline context
    contextSupplier.forCurrentThread()
        .withHeaders(new HashMap<>())
        .execute(() -> {
          injector.beforeExecute(httpClient, requestBuilder, request);
          assertEquals(String.valueOf(requestTimeout), request.getHeaders().get(X_DEADLINE_TIMEOUT_MS));
          assertEquals(String.valueOf(requestTimeout), request.getHeaders().get(X_OUTER_TIMEOUT_MS));
          assertEquals(requestTimeout, requestBuilder.build().getRequestTimeout());
        });
  }

  // Tests from DeadlineContextCheckTest
  @Test
  public void testOnRequestDeadlineExceeded() {
    // Create context with deadline already passed
    Map<String, List<String>> headers = Map.of(
        X_DEADLINE_TIMEOUT_MS, List.of("50")
    );

    HttpClientContext context = new HttpClientContext(
        OffsetDateTime.now().minusNanos(100_000_000L), // Started 100ms ago
        headers,
        Collections.emptyMap(),
        Collections.emptyList(),
        StorageUtils.build(List.of())
    );

    DeadlineCheckerAndPropagator check = new DeadlineCheckerAndPropagator(() -> context);

    // Should throw exception
    RuntimeException exception = assertThrows(
        RuntimeException.class, () -> {
          check.beforeExecute(httpClient, new RequestBuilder(), new TestRequest());
        }
    );
  }

  @Test
  public void testOnRequestNoDeadlineExceeded() {
    // Create context with deadline still valid
    int requestTimeout = 235;

    Map<String, List<String>> headers = Map.of(
        X_DEADLINE_TIMEOUT_MS, List.of("1000")
    );

    HttpClientContext context = new HttpClientContext(
        headers,
        Collections.emptyMap(),
        Collections.emptyList()
    );

    DeadlineCheckerAndPropagator check = new DeadlineCheckerAndPropagator(() -> context);

    // Create request with timeout
    Request request = new RequestBuilder()
        .setUrl("http://localhost/test")
        .setRequestTimeout(requestTimeout)
        .build();

    RequestBuilder requestBuilder = new RequestBuilder(request);

    // Should not throw exception
    check.beforeExecute(httpClient, requestBuilder, request);
    
    // Verify headers are present
    assertEquals(String.valueOf(requestTimeout), request.getHeaders().get(X_DEADLINE_TIMEOUT_MS));
    assertEquals(String.valueOf(requestTimeout), request.getHeaders().get(X_OUTER_TIMEOUT_MS));
    assertEquals(requestTimeout, requestBuilder.build().getRequestTimeout());
  }

  @Test
  public void testNullContextSupplier() {
    Supplier<HttpClientContext> nullSupplier = () -> null;
    DeadlineCheckerAndPropagator check = new DeadlineCheckerAndPropagator(nullSupplier);

    // Create request with timeout
    Request request = new RequestBuilder()
        .setUrl("http://localhost/test")
        .setRequestTimeout(500)
        .build();

    RequestBuilder requestBuilder = new RequestBuilder(request);

    // Should not throw exception when context is null
    check.beforeExecute(httpClient, requestBuilder, request);
    
    // Verify request timeout is preserved when context supplier is null
    assertEquals(500, requestBuilder.build().getRequestTimeout());
  }

  @Test
  public void testDeadlineDisabled() {
    Map<String, List<String>> headers = Map.of(
        X_DEADLINE_TIMEOUT_MS, List.of("50")
    );

    // Create request with timeout and disabled deadline
    Request request = new RequestBuilder()
        .setUrl("http://localhost/test")
        .setRequestTimeout(500)
        .setDeadlineEnabled(false)
        .build();

    RequestBuilder requestBuilder = new RequestBuilder(request);

    // Execute with context
    contextSupplier.forCurrentThread()
        .withHeaders(headers)
        .execute(() -> {
          injector.beforeExecute(httpClient, requestBuilder, request);

          assertNull(request.getHeaders().get(X_DEADLINE_TIMEOUT_MS),
              "X_DEADLINE_TIMEOUT_MS header should not be present in request when deadline is disabled");
          assertNull(request.getHeaders().get(X_OUTER_TIMEOUT_MS),
              "X_OUTER_TIMEOUT_MS header should not be present when deadline is disabled");

          Request requestBuilded = requestBuilder.build();
          assertEquals(500, requestBuilded.getRequestTimeout());
          assertFalse(requestBuilded.isDeadlineEnabled());
        });
  }

  @Test
  public void testExternalRequest() {
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(X_DEADLINE_TIMEOUT_MS, singletonList("300"));

    // Create external request with timeout
    Request request = new RequestBuilder()
        .setUrl("http://localhost/test")
        .setRequestTimeout(500)
        .build();
    when(httpClient.isExternalRequest()).thenReturn(true);
    RequestBuilder requestBuilder = new RequestBuilder(request);

    // Execute with context
    contextSupplier.forCurrentThread()
        .withHeaders(headers)
        .execute(() -> {
          injector.beforeExecute(httpClient, requestBuilder, request);

          assertNull(request.getHeaders().get(X_DEADLINE_TIMEOUT_MS));
          assertNull(request.getHeaders().get(X_OUTER_TIMEOUT_MS));
          int requestTimeoutLeft = requestBuilder.build().getRequestTimeout();
          assertTrue(requestTimeoutLeft > 10 && requestTimeoutLeft <= 300);
        });
  }

  // Simple test classes to avoid mocking
  private static class TestRequest extends Request {
    public TestRequest() {
      super(new org.asynchttpclient.RequestBuilder("GET").setUrl("http://localhost/test").build(), false, true);
    }
  }
}
