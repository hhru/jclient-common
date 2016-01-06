package ru.hh.jclient.common;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.RequestDebug;

/**
 * Creates and removes {@link HttpClientContext} using ThreadLocal to store it.
 */
public class HttpClientContextThreadLocalSupplier implements Supplier<HttpClientContext> {

  private static final Logger LOG = LoggerFactory.getLogger(HttpClientContextThreadLocalSupplier.class);

  private static final ThreadLocal<HttpClientContext> storage = ThreadLocal.withInitial(() -> null);

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

  /**
   * Set context for current thread. Silently overwrites existing context if any.
   * 
   * @param context
   *          context to install
   */
  static void installContext(HttpClientContext context) {
    if (storage.get() != null) {
      LOG.warn("Unexpected context when installing. Replacing {} with {}", storage.get(), context);
    }
    storage.set(context);
  }

  /**
   * Remove specified context. Logs if existing context is different to provided one.
   * 
   * @param context
   *          context to remove
   */
  static void removeContext(HttpClientContext context) {
    if (storage.get() != context) {
      LOG.warn("Unexpected context when removing {} - was {}", context, storage.get());
    }
    else if (storage.get() == null) {
      LOG.warn("Unexpected context when removing {} - null", context);
    }
    storage.remove();
  }
}
