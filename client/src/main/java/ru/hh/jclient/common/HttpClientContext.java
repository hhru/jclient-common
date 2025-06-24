package ru.hh.jclient.common;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import static java.util.Collections.emptySet;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.TreeMap;
import java.util.function.Supplier;
import ru.hh.deadline.context.DeadlineContext;
import static ru.hh.jclient.common.RequestUtils.isInDebugMode;
import ru.hh.jclient.common.util.storage.StorageUtils;
import ru.hh.jclient.common.util.storage.StorageUtils.Storages;

/**
 * Context of global (incoming) request, that is going to spawn local (outgoing) request using
 * {@link HttpClientFactory#with(Request)}.
 */
public class HttpClientContext {

  private final OffsetDateTime requestStart;
  private final Map<String, List<String>> headers;
  private final boolean debugMode;
  private final List<Supplier<HttpClientEventListener>> eventListenerSuppliers;
  private final Storages storages;
  private final DeadlineContext deadlineContext;

  public HttpClientContext(
      Map<String, List<String>> headers,
      Map<String, List<String>> queryParams,
      List<Supplier<HttpClientEventListener>> eventListenerSuppliers
  ) {
    this(headers, queryParams, eventListenerSuppliers, StorageUtils.build(emptySet()));
  }

  public HttpClientContext(
      Map<String, List<String>> headers,
      Map<String, List<String>> queryParams,
      List<Supplier<HttpClientEventListener>> eventListenerSuppliers,
      Storages storages) {
    this(OffsetDateTime.now(), headers, queryParams, eventListenerSuppliers, storages);
  }

  /**
   * Creates context.
   *
   * @param requestStart time of global request start
   * @param headers headers of global request. Some of them can be used by the local request.
   * @param queryParams query params of global request
   * @param eventListenerSuppliers list of suppliers of object used to gather event listener information
   * @param storages object storages that needs to be transferred to ning threads executing requests and completable future chains
   */
  public HttpClientContext(
      OffsetDateTime requestStart,
      Map<String, List<String>> headers,
      Map<String, List<String>> queryParams,
      List<Supplier<HttpClientEventListener>> eventListenerSuppliers,
      Storages storages) {
    this.requestStart = requestStart;
    requireNonNull(headers, "headers must not be null");
    this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    this.headers.putAll(headers);

    this.debugMode = isInDebugMode(headers, queryParams);
    this.eventListenerSuppliers = new ArrayList<>(eventListenerSuppliers);
    this.storages = requireNonNull(storages, "storages must not be null");
    this.deadlineContext = DeadlineContext.createDeadlineContext(
        requestStart,
        RequestUtils.getLongHeaderValue(headers, HttpHeaderNames.X_DEADLINE_TIMEOUT_MS).orElse(null),
        RequestUtils.getLongHeaderValue(headers, HttpHeaderNames.X_OUTER_TIMEOUT_MS).orElse(null));
  }

  public OffsetDateTime getRequestStart() {
    return requestStart;
  }

  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  public boolean isDebugMode() {
    return this.debugMode;
  }

  List<Supplier<HttpClientEventListener>> getEventListenerSuppliers() {
    return eventListenerSuppliers;
  }

  void withEventListenerSupplier(Supplier<HttpClientEventListener> eventListenerSupplier) {
    this.eventListenerSuppliers.add(eventListenerSupplier);
  }

  void removeEventListenerSupplier(Supplier<HttpClientEventListener> eventListenerSupplier) {
    this.eventListenerSuppliers.remove(eventListenerSupplier);
  }

  Storages getStorages() {
    return storages;
  }

  public DeadlineContext getDeadlineContext() {
    return deadlineContext;
  }
}
