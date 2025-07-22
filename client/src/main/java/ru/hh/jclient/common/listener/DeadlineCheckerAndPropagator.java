package ru.hh.jclient.common.listener;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.HttpClientContextThreadLocalSupplier;
import ru.hh.jclient.common.HttpClientEventListener;
import ru.hh.jclient.common.HttpHeaderNames;
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
          long timeLeft = deadlineContext.getTimeLeft();
          deadlineContext.checkAndThrowDeadline();
          // Use the minimum of request timeout and deadline timeLeft
          timeLeft = Math.min(request.getRequestTimeout(), timeLeft);
          requestBuilder.setRequestTimeout((int) timeLeft);
          if (request.isExternalRequest() || requestBuilder.isDeadlineEnabled()) {
            setHeaders(String.valueOf(timeLeft), String.valueOf(request.getRequestTimeout()), contextSupplier.get());
          }
        });
  }

  private void setHeaders(String estimateValue, String requestTimeout, HttpClientContext context) {
    Map<String, List<String>> headers = context.getHeaders();
    headers.put(HttpHeaderNames.X_DEADLINE_TIMEOUT_MS, List.of(estimateValue));
    headers.put(HttpHeaderNames.X_OUTER_TIMEOUT_MS, List.of(requestTimeout));
  }
} 
