package ru.hh.jclient.common;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.RequestDebug;

/**
 * Creates and removes {@link HttpClientContext} using ThreadLocal to store it.
 */
public class HttpClientContextThreadLocalSupplier implements Supplier<HttpClientContext> {

  private ThreadLocal<HttpClientContext> storage = ThreadLocal.withInitial(() -> null);

  private Supplier<RequestDebug> requestDebugSupplier;

  public HttpClientContextThreadLocalSupplier(Supplier<RequestDebug> requestDebugSupplier) {
    this.requestDebugSupplier = requestDebugSupplier;
  }

  /**
   * Retrieves current context, can be null.
   */
  @Override
  public HttpClientContext get() {
    return storage.get();
  }

  /**
   * Creates and stores new context using provided headers. This method should be called before any invocations of jclient implementation in the
   * current thread.
   */
  public void addContext(Map<String, List<String>> headers, Map<String, List<String>> queryParams) {
    storage.set(new HttpClientContext(headers, queryParams, requestDebugSupplier));
  }

  /**
   * Discards and removes current context from storage. After calling this method any invocations of jclient implementations will raise an error.
   */
  public void removeContext() {
    storage.remove();
  }
}
