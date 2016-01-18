package ru.hh.jclient.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.RequestDebug;
import ru.hh.jclient.common.util.storage.TransferableSupplier;
import ru.hh.jclient.common.util.storage.TransferableThreadLocalSupplier;

/**
 * Creates and removes {@link HttpClientContext} using ThreadLocal to store it.
 */
public class HttpClientContextThreadLocalSupplier extends TransferableThreadLocalSupplier<HttpClientContext> {

  private static final Logger LOG = LoggerFactory.getLogger(HttpClientContextThreadLocalSupplier.class);

  @Override
  public Logger getLogger() {
    return LOG;
  }

  private Supplier<RequestDebug> requestDebugSupplier;
  private Collection<TransferableSupplier<?>> transferableSuppliers = new ArrayList<>();

  public HttpClientContextThreadLocalSupplier(Supplier<RequestDebug> requestDebugSupplier) {
    this.requestDebugSupplier = requestDebugSupplier;
  }

  public HttpClientContextThreadLocalSupplier add(TransferableSupplier<?> supplier) {
    this.transferableSuppliers.add(supplier);
    return this;
  }

  /**
   * Creates and stores new context using provided headers. This method should be called before any invocations of jclient implementation in the
   * current thread.
   */
  public void addContext(Map<String, List<String>> headers, Map<String, List<String>> queryParams) {
    set(new HttpClientContext(headers, queryParams, requestDebugSupplier, transferableSuppliers));
  }
}
