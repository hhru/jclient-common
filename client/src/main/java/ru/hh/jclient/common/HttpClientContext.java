package ru.hh.jclient.common;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static ru.hh.jclient.common.RequestUtils.isInDebugMode;
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

  private Map<String, List<String>> headers;
  private boolean debugMode;
  private Supplier<RequestDebug> debugSupplier;
  private Optional<String> requestId;
  private Storages storages;

  public HttpClientContext(Map<String, List<String>> headers, Map<String, List<String>> queryParams, Supplier<RequestDebug> debugSupplier) {
    this(headers, queryParams, debugSupplier, StorageUtils.build(emptySet()));
  }

  /**
   * Creates context.
   *
   * @param headers headers of global request. Some of them can be used by the local request.
   * @param queryParams query params of global request
   * @param debugSupplier supplier of object used to gather debug information
   * @param storages object storages that needs to be transferred to ning threads executing requests and completable future chains
   */
  public HttpClientContext(
      Map<String, List<String>> headers,
      Map<String, List<String>> queryParams,
      Supplier<RequestDebug> debugSupplier,
      Storages storages) {
    requireNonNull(headers, "headers must not be null");
    this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    this.headers.putAll(headers);

    this.debugMode = isInDebugMode(headers, queryParams);
    this.debugSupplier = requireNonNull(debugSupplier, "debugSupplier must not be null");
    this.requestId = RequestUtils.getRequestId(headers);
    this.storages = requireNonNull(storages, "storages must not be null");
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
    return "HttpClientContext for " + requestId.orElse("unknown") + " requestId (" + this.hashCode() + ")";
  }
}
