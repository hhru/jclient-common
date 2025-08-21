package ru.hh.jclient.common.balancing;

import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;
import ru.hh.jclient.common.RequestStrategy;
import ru.hh.jclient.common.Response;

public class ExternalUrlRequestorTest {
  private ExternalUrlRequestor externalUrlRequestor;
  private RequestStrategy.RequestExecutor requestExecutor = mock(RequestStrategy.RequestExecutor.class);

  @BeforeEach
  public void setUp() {
    RetryPolicy retryPolicy = new RetryPolicy();
    retryPolicy.update(Map.of(500, true)); // Configure rules for testing

    Request request = new RequestBuilder().setUrl("https://test-upstream").build();

    externalUrlRequestor = new ExternalUrlRequestor(
        null, // upstream
        request,
        requestExecutor,
        retryPolicy,
        1000, // requestTimeoutMs
        3,    // maxRequestTimeoutTries
        5,    // maxTries
        1.0,  // timeoutMultiplier
        "INFO", // balancingRequestsLogLevel
        false, // forceIdempotence
        Set.of() // monitorings
    );
  }

  @Test
  public void testRetryPolicyIsUsed() {
    Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(500); // Simulate a server error
    assertTrue(externalUrlRequestor.checkRetry(response, false)); // Assert that the retry policy allows retry
    when(response.getStatusCode()).thenReturn(599); // Simulate a server error
    assertFalse(externalUrlRequestor.checkRetry(response, false));
  }
}
