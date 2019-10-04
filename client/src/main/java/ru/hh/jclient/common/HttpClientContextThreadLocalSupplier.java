package ru.hh.jclient.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import ru.hh.jclient.common.util.storage.Storage;
import ru.hh.jclient.common.util.storage.ThreadLocalStorage;
import ru.hh.jclient.common.util.storage.StorageUtils;
import ru.hh.jclient.common.util.storage.StorageUtils.Storages;
import static java.util.Optional.ofNullable;

/**
 * Creates and removes {@link HttpClientContext} using ThreadLocal to store it.
 */
public class HttpClientContextThreadLocalSupplier extends ThreadLocalStorage<HttpClientContext> {

  private final List<Supplier<RequestDebug>> requestDebugSuppliers;
  private final Storages storagesForTransfer;

  public HttpClientContextThreadLocalSupplier() {
    this.storagesForTransfer = StorageUtils.build(Collections.emptySet());
    this.requestDebugSuppliers = new CopyOnWriteArrayList<>();
  }

  /**
   * Add storage to be used in all contexts.
   *
   * @param storage storage to add
   * @return this instance
   */
  public HttpClientContextThreadLocalSupplier register(Storage<?> storage) {
    this.storagesForTransfer.add(storage);
    return this;
  }

  /**
   * Add request debug supplier to be used in all contexts.
   *
   * @param requestDebugSupplier supplier to add
   * @return this instance
   */
  public HttpClientContextThreadLocalSupplier registerRequestDebugSupplier(Supplier<RequestDebug> requestDebugSupplier) {
    this.requestDebugSuppliers.add(requestDebugSupplier);
    return this;
  }

  /**
   * Add request debug supplier to client context of current thread. Won't do anything if context is not set yet.
   *
   * @param requestDebugSupplier supplier to add
   * @return this instance
   */
  public HttpClientContextThreadLocalSupplier registerRequestDebugSupplierForCurrentContext(Supplier<RequestDebug> requestDebugSupplier) {
    ofNullable(get()).ifPresent(context -> context.addDebugSupplier(requestDebugSupplier));
    return this;
  }

  /**
   * Remove request debug supplier from client context of current thread. Won't do anything if context is not set yet or
   * if supplier wasn't added previously.
   *
   * @param requestDebugSupplier supplier to remove
   * @return this instance
   */
  public HttpClientContextThreadLocalSupplier removeRequestDebugSupplierFromCurrentContext(Supplier<RequestDebug> requestDebugSupplier) {
    ofNullable(get()).ifPresent(context -> context.removeDebugSupplier(requestDebugSupplier));
    return this;
  }

  /**
   * Creates and stores new context using provided headers. This method should be called before any invocations of jclient implementation in the
   * current thread.
   */
  public void addContext(Map<String, List<String>> headers, Map<String, List<String>> queryParams) {
    set(new HttpClientContext(headers, queryParams, requestDebugSuppliers, storagesForTransfer));
  }
}
