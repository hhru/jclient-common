package ru.hh.jclient.common.listener;

import java.util.Optional;
import java.util.function.Supplier;
import ru.hh.deadline.context.DeadlineContext;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.jclient.common.HttpClientEventListener;
import ru.hh.jclient.common.HttpHeaderNames;
import ru.hh.jclient.common.HttpHeaders;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;

/**
 * Injects deadline timeout estimates into request headers.
 *
 * <p>Updates the X-Deadline-timeout-ms header with remaining time from {@link DeadlineContext}
 * and adjusts request timeout to the minimum of original timeout and deadline estimate.
 */
public class DeadlineCheckerAndPropagator implements HttpClientEventListener {

  private final Supplier<HttpClientContext> contextSupplier;

  public DeadlineCheckerAndPropagator(Supplier<HttpClientContext> contextSupplier) {
    this.contextSupplier = contextSupplier;
  }

  /**
   * Alternative constructor that accepts HttpClientContextThreadLocalSupplier
   */
  public DeadlineCheckerAndPropagator(HttpClientContextThreadLocalSupplier contextSupplier) {
    this.contextSupplier = contextSupplier::get;
  }

  @Override
  public void beforeExecute(HttpClient httpClient, RequestBuilder requestBuilder, Request request) {
    Optional.ofNullable(contextSupplier.get())
        .map(HttpClientContext::getDeadlineContext)
        .ifPresent(deadlineContext -> {
          deadlineContext.checkAndThrowDeadline();
          if (!request.isExternalRequest() && requestBuilder.isDeadlineEnabled()) {
            // Use the minimum of request timeout and deadline timeLeft
            long timeLeft = getTimeLeft(deadlineContext, request);
            requestBuilder.setRequestTimeout((int) timeLeft);
            setHeaders(String.valueOf(timeLeft), String.valueOf(timeLeft), request);
          }
        });
  }

  private void setHeaders(String estimateValue, String requestTimeout, Request request) {
    HttpHeaders headers = request.getHeaders();
    headers.add(HttpHeaderNames.X_DEADLINE_TIMEOUT_MS, estimateValue);
    headers.add(HttpHeaderNames.X_OUTER_TIMEOUT_MS, requestTimeout);
  }

  private long getTimeLeft(DeadlineContext deadlineContext, Request request) {
    long timeLeft = deadlineContext.getTimeLeft();
    if (timeLeft > -1) {
      return Math.min(request.getRequestTimeout(), timeLeft);
    } else {
      return request.getRequestTimeout();
    }
  }
} 
