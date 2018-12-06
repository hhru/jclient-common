package ru.hh.jclient.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import ru.hh.jclient.common.util.storage.Storage;
import ru.hh.jclient.common.util.storage.ThreadLocalStorage;
import ru.hh.jclient.common.util.storage.StorageUtils;
import ru.hh.jclient.common.util.storage.StorageUtils.Storages;

/**
 * Creates and removes {@link HttpClientContext} using ThreadLocal to store it.
 */
public class HttpClientContextThreadLocalSupplier extends ThreadLocalStorage<HttpClientContext> {

  private Supplier<RequestDebug> requestDebugSupplier;
  private Storages storagesForTransfer;

  public HttpClientContextThreadLocalSupplier(Supplier<RequestDebug> requestDebugSupplier) {
    this.requestDebugSupplier = requestDebugSupplier;
    this.storagesForTransfer = StorageUtils.build(Collections.emptySet());
  }

  public HttpClientContextThreadLocalSupplier register(Storage<?> supplier) {
    this.storagesForTransfer.add(supplier);
    return this;
  }

  /**
   * Creates and stores new context using provided headers. This method should be called before any invocations of jclient implementation in the
   * current thread.
   */
  public void addContext(Map<String, List<String>> headers, Map<String, List<String>> queryParams) {
    set(new HttpClientContext(headers, queryParams, requestDebugSupplier, storagesForTransfer));
  }
}
