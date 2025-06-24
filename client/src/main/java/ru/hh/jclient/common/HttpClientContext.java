package ru.hh.jclient.common;

import java.time.LocalDateTime;
import java.util.ArrayList;
import static java.util.Collections.emptySet;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.TreeMap;
import java.util.function.Supplier;
import static ru.hh.jclient.common.RequestUtils.isInDebugMode;
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
  private final List<Supplier<RequestDebug>> debugSuppliers;
  private final Storages storages;

  public HttpClientContext(Map<String, List<String>> headers, Map<String, List<String>> queryParams, List<Supplier<RequestDebug>> debugSuppliers) {
    this(headers, queryParams, debugSuppliers, StorageUtils.build(emptySet()));
  }

  public HttpClientContext(
      Map<String, List<String>> headers,
      Map<String, List<String>> queryParams,
      List<Supplier<RequestDebug>> debugSuppliers,
      Storages storages) {
    this(LocalDateTime.now(), headers, queryParams, debugSuppliers, storages);
  }

  /**
   * Creates context.
   *
   * @param requestStart time of global request start
   * @param headers headers of global request. Some of them can be used by the local request.
   * @param queryParams query params of global request
   * @param debugSuppliers list of suppliers of object used to gather debug information
   * @param storages object storages that needs to be transferred to ning threads executing requests and completable future chains
   */
  public HttpClientContext(
      LocalDateTime requestStart,
      Map<String, List<String>> headers,
      Map<String, List<String>> queryParams,
      List<Supplier<RequestDebug>> debugSuppliers,
      Storages storages) {
    this.requestStart = requestStart;
    requireNonNull(headers, "headers must not be null");
    this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    this.headers.putAll(headers);

    this.debugMode = isInDebugMode(headers, queryParams);
    this.debugSuppliers = new ArrayList<>(debugSuppliers);
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

  List<Supplier<RequestDebug>> getDebugSuppliers() {
    return debugSuppliers;
  }

  void addDebugSupplier(Supplier<RequestDebug> requestDebugSupplier) {
    this.debugSuppliers.add(requestDebugSupplier);
  }

  void removeDebugSupplier(Supplier<RequestDebug> requestDebugSupplier) {
    this.debugSuppliers.remove(requestDebugSupplier);
  }

  Storages getStorages() {
    return storages;
  }
}
