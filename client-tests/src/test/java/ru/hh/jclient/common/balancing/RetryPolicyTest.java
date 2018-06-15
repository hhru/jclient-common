package ru.hh.jclient.common.balancing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_CONNECT_ERROR;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_REQUEST_TIMEOUT;

public class RetryPolicyTest {

  @Test
  public void testIsRetriable() {

    RetryPolicy policy = new RetryPolicy();

    assertTrue(policy.isRetriable(STATUS_CONNECT_ERROR, "GET"));
    assertTrue(policy.isRetriable(STATUS_REQUEST_TIMEOUT, "GET"));
    assertFalse(policy.isRetriable(STATUS_REQUEST_TIMEOUT, "POST"));

    policy.update("non_idempotent_503");

    assertFalse(policy.isRetriable(STATUS_CONNECT_ERROR, "GET"));
    assertFalse(policy.isRetriable(STATUS_REQUEST_TIMEOUT, "GET"));
    assertTrue(policy.isRetriable(STATUS_REQUEST_TIMEOUT, "POST"));
  }
}
