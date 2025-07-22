package ru.hh.jclient.common;

import java.time.OffsetDateTime;
import java.util.Collections;
import static java.util.Collections.singletonList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static ru.hh.jclient.common.HttpHeaderNames.X_DEADLINE_TIMEOUT_MS;
import static ru.hh.jclient.common.HttpHeaderNames.X_OUTER_TIMEOUT_MS;
import ru.hh.jclient.common.listener.DeadlineCheckerAndPropagator;
import ru.hh.jclient.common.util.storage.StorageUtils;

public class DeadlineCheckerAndPropagatorTest {

  private DeadlineCheckerAndPropagator injector;
  private HttpClientContextThreadLocalSupplier contextSupplier;

  @Before
  public void setUp() {
    contextSupplier = new HttpClientContextThreadLocalSupplier();
    injector = new DeadlineCheckerAndPropagator(contextSupplier);
  }

  @Test
  public void testChooseRequestTimeout() {
    // Setup context with deadline
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(X_DEADLINE_TIMEOUT_MS, singletonList("1000"));

    // Create request with timeout
    Request request = new RequestBuilder()
        .setUrl("http://localhost/test")
        .setRequestTimeout(500)
        .build();

    RequestBuilder requestBuilder = new RequestBuilder(request);

    // Execute with context
    contextSupplier.forCurrentThread()
        .withHeaders(headers)
        .execute(() -> {
          injector.beforeExecute(null, requestBuilder, request);

          // Verify header was injected with correct value
          assertEquals("500", contextSupplier.get().getHeaders().get(X_DEADLINE_TIMEOUT_MS).get(0));
        });
  }

  @Test
  public void testChooseDeadlineHeader() {
    // Setup context with deadline
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(X_DEADLINE_TIMEOUT_MS, singletonList("100"));

    // Create request with timeout
    Request request = new RequestBuilder()
        .setUrl("http://localhost/test")
        .setRequestTimeout(200)
        .build();

    RequestBuilder requestBuilder = new RequestBuilder(request);

    // Execute with context
    contextSupplier.forCurrentThread()
        .withHeaders(headers)
        .execute(() -> {
          injector.beforeExecute(null, requestBuilder, request);

          // Verify header was injected with correct value (min of deadline and request timeout)
          int header = Integer.parseInt(contextSupplier.get().getHeaders().get(X_DEADLINE_TIMEOUT_MS).get(0));
          assertTrue(header > 10 && header <= 100);
        });
  }

  @Test
  public void testChooseOuterHeader() {
    // Setup context with deadline
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(X_OUTER_TIMEOUT_MS, singletonList("100"));

    // Create request with timeout
    Request request = new RequestBuilder()
        .setUrl("http://localhost/test")
        .setRequestTimeout(200)
        .build();

    RequestBuilder requestBuilder = new RequestBuilder(request);

    // Execute with context
    contextSupplier.forCurrentThread()
        .withHeaders(headers)
        .execute(() -> {
          injector.beforeExecute(null, requestBuilder, request);

          // Verify header was injected with correct value (min of deadline and request timeout)
          int header = Integer.parseInt(contextSupplier.get().getHeaders().get(X_DEADLINE_TIMEOUT_MS).get(0));
          assertTrue(header > 10 && header <= 100);
        });
  }

  @Test
  public void testRetryDeadlineHeaderInjection() {
    // Setup context with deadline
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(X_DEADLINE_TIMEOUT_MS, singletonList("1000"));

    // Create request with timeout using RequestBuilder
    Request request = new RequestBuilder()
        .setUrl("http://localhost/test")
        .setRequestTimeout(500)
        .build();

    RequestBuilder requestBuilder = new RequestBuilder(request);

    // Execute with context
    contextSupplier.forCurrentThread()
        .withHeaders(headers)
        .execute(() -> {
          injector.beforeExecute(null, requestBuilder, request);

          // Verify header was injected with correct value
          assertEquals("500", contextSupplier.get().getHeaders().get(X_DEADLINE_TIMEOUT_MS).get(0));
        });
  }

  @Test
  public void testNoDeadlineContext() {
    // Create request with timeout using RequestBuilder
    Request request = new RequestBuilder()
        .setUrl("http://localhost/test")
        .setRequestTimeout(500)
        .build();

    RequestBuilder requestBuilder = new RequestBuilder(request);

    // Execute without deadline context
    contextSupplier.forCurrentThread()
        .withHeaders(new HashMap<>())
        .execute(() -> {
          // Should not throw exception when no deadline context
          injector.beforeExecute(null, requestBuilder, request);
        });
  }

  @Test
  public void testNullContext() {
    // Create request with timeout using RequestBuilder
    Request request = new RequestBuilder()
        .setUrl("http://localhost/test")
        .setRequestTimeout(500)
        .build();

    RequestBuilder requestBuilder = new RequestBuilder(request);

    // Should not throw exception when context is null
    injector.beforeExecute(null, requestBuilder, request);
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
          check.beforeExecute(null, new RequestBuilder(), new TestRequest());
        }
    );
  }

  @Test
  public void testOnRequestNoDeadlineExceeded() {
    // Create context with deadline still valid
    Map<String, List<String>> headers = Map.of(
        X_DEADLINE_TIMEOUT_MS, List.of("1000")
    );

    HttpClientContext context = new HttpClientContext(
        headers,
        Collections.emptyMap(),
        Collections.emptyList()
    );

    DeadlineCheckerAndPropagator check = new DeadlineCheckerAndPropagator(() -> context);

    // Should not throw exception
    check.beforeExecute(null, new RequestBuilder(), new TestRequest());
  }

  @Test
  public void testOnRetryDeadlineExceeded() {
    // Create context with deadline already passed
    Map<String, List<String>> headers = Map.of(
        X_DEADLINE_TIMEOUT_MS, List.of("30")
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
          check.beforeExecute(null, new RequestBuilder(), new TestRequest());
        }
    );
  }

  @Test
  public void testOnRetryNoDeadlineExceeded() {
    // Create context with deadline still valid
    Map<String, List<String>> headers = Map.of(
        X_DEADLINE_TIMEOUT_MS, List.of("500")
    );

    HttpClientContext context = new HttpClientContext(
        headers,
        Collections.emptyMap(),
        Collections.emptyList()
    );

    DeadlineCheckerAndPropagator check = new DeadlineCheckerAndPropagator(() -> context);

    // Should not throw exception
    check.beforeExecute(null, new RequestBuilder(), new TestRequest());
  }

  @Test
  public void testNullContextSupplier() {
    Supplier<HttpClientContext> nullSupplier = () -> null;
    DeadlineCheckerAndPropagator check = new DeadlineCheckerAndPropagator(nullSupplier);

    // Should not throw exception when context is null
    check.beforeExecute(null, new RequestBuilder(), new TestRequest());
  }

  @Test
  public void testNoDeadlineHeaders() {
    // Create context without deadline headers
    HttpClientContext context = new HttpClientContext(
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyList()
    );

    DeadlineCheckerAndPropagator check = new DeadlineCheckerAndPropagator(() -> context);

    // Should not throw exception when no deadline is set
    check.beforeExecute(null, new RequestBuilder(), new TestRequest());
  }

  @Test
  public void testDeadlineDisabled() {
    // Create request with timeout and disabled deadline
    Request request = new RequestBuilder()
        .setUrl("http://localhost/test")
        .setRequestTimeout(500)
        .build();

    RequestBuilder requestBuilder = new RequestBuilder(request)
        .setDeadlineEnabled(false);

    // Execute with context
    contextSupplier.forCurrentThread()
        .execute(() -> {
          injector.beforeExecute(null, requestBuilder, request);

          assertNull(contextSupplier.get().getHeaders().get(X_DEADLINE_TIMEOUT_MS));
          assertNull(contextSupplier.get().getHeaders().get(X_OUTER_TIMEOUT_MS));
        });
  }

  // Simple test classes to avoid mocking
  private static class TestRequest extends Request {
    public TestRequest() {
      super(new org.asynchttpclient.RequestBuilder("GET").setUrl("http://localhost/test").build(), false);
    }
  }
}
