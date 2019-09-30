package ru.hh.jclient.common;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static ru.hh.jclient.common.RequestUtils.isInDebugMode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;
import ru.hh.jclient.common.util.storage.StorageUtils;
import ru.hh.jclient.common.util.storage.StorageUtils.Storages;

/**
 * Context of global (incoming) request, that is going to spawn local (outgoing) request using
 * {@link HttpClientFactory#with(Request)}.
 */
public class HttpClientContext {

  private final LocalDateTime requestStart;
  private final Map<String, List<String>> headers;
  private final boolean debugMode;
  private final Supplier<RequestDebug> debugSupplier;
  private final Optional<String> requestId;
  private final Storages storages;

  public HttpClientContext(Map<String, List<String>> headers, Map<String, List<String>> queryParams, Supplier<RequestDebug> debugSupplier) {
    this(headers, queryParams, debugSupplier, StorageUtils.build(emptySet()));
  }

  public HttpClientContext(
      Map<String, List<String>> headers,
      Map<String, List<String>> queryParams,
      Supplier<RequestDebug> debugSupplier,
      Storages storages) {
    this(LocalDateTime.now(), headers, queryParams, debugSupplier, storages);
  }

  /**
   * Creates context.
   *
   * @param requestStart time of global request start
   * @param headers headers of global request. Some of them can be used by the local request.
   * @param queryParams query params of global request
   * @param debugSupplier supplier of object used to gather debug information
   * @param storages object storages that needs to be transferred to ning threads executing requests and completable future chains
   */
  public HttpClientContext(
      LocalDateTime requestStart,
      Map<String, List<String>> headers,
      Map<String, List<String>> queryParams,
      Supplier<RequestDebug> debugSupplier,
      Storages storages) {
    this.requestStart = requestStart;
    requireNonNull(headers, "headers must not be null");
    this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    this.headers.putAll(headers);

    this.debugMode = isInDebugMode(headers, queryParams);
    this.debugSupplier = ofNullable(debugSupplier).orElseGet(() -> () -> RequestDebug.DISABLED);
    this.requestId = RequestUtils.getRequestId(headers);
    this.storages = requireNonNull(storages, "storages must not be null");
  }

  public LocalDateTime getRequestStart() {
    return requestStart;
  }

  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  public boolean isDebugMode() {
    return this.debugMode;
  }

  public Supplier<RequestDebug> getDebugSupplier() {
    return debugSupplier;
  }

  public Storages getStorages() {
    return storages;
  }

  @Override
  public String toString() {
    return "HttpClientContext for " + requestId.orElse("unknown") + " requestId (" + this.hashCode() + ')';
  }
}
