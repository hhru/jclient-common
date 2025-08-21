package ru.hh.jclient.common.balancing;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static ru.hh.jclient.common.HttpStatuses.CONNECT_TIMEOUT_ERROR;
import static ru.hh.jclient.common.HttpStatuses.SERVICE_UNAVAILABLE;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResponseMock;
import static ru.hh.jclient.common.ResponseStatusMessages.CONNECT_ERROR_MESSAGE;
import static ru.hh.jclient.common.ResponseStatusMessages.REQUEST_TIMEOUT_MESSAGE;


public class RetryPolicyTest {

  @Test
  public void testDefault() {
    RetryPolicy policy = new RetryPolicy();

    assertTrue(policy.isRetriable(createResponse(CONNECT_TIMEOUT_ERROR, CONNECT_ERROR_MESSAGE), true));
    assertTrue(policy.isRetriable(createResponse(CONNECT_TIMEOUT_ERROR, CONNECT_ERROR_MESSAGE), false));
    assertTrue(policy.isRetriable(createResponse(SERVICE_UNAVAILABLE, ""), true));
    assertFalse(policy.isRetriable(createResponse(SERVICE_UNAVAILABLE, ""), false));
    assertTrue(policy.isRetriable(createResponse(CONNECT_TIMEOUT_ERROR, REQUEST_TIMEOUT_MESSAGE), true));
    assertFalse(policy.isRetriable(createResponse(CONNECT_TIMEOUT_ERROR, REQUEST_TIMEOUT_MESSAGE), false));
  }

  @Test
  public void testNonIdempotentRetry() {
    RetryPolicy policy = new RetryPolicy();
    Map<Integer, Boolean> retryPolicy = Map.of(503, true);

    policy.update(retryPolicy);

    assertTrue(policy.isRetriable(createResponse(CONNECT_TIMEOUT_ERROR, CONNECT_ERROR_MESSAGE), true));
    assertTrue(policy.isRetriable(createResponse(CONNECT_TIMEOUT_ERROR, CONNECT_ERROR_MESSAGE), false));
    assertTrue(policy.isRetriable(createResponse(SERVICE_UNAVAILABLE, ""), true));
    assertTrue(policy.isRetriable(createResponse(SERVICE_UNAVAILABLE, ""), false));
    assertFalse(policy.isRetriable(createResponse(CONNECT_TIMEOUT_ERROR, REQUEST_TIMEOUT_MESSAGE), true));
    assertFalse(policy.isRetriable(createResponse(CONNECT_TIMEOUT_ERROR, REQUEST_TIMEOUT_MESSAGE), false));
  }

  private static Response createResponse(int code, String message) {
    ResponseMock response = new ResponseMock();
    response.setStatusCode(code);
    response.setStatusText(message);

    return response;
  }
}
