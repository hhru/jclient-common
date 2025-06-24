package ru.hh.jclient.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.Optional.ofNullable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import static ru.hh.jclient.common.HttpHeaderNames.X_REQUEST_ID;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableRunnable;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableSupplier;
import ru.hh.jclient.common.util.storage.Storage;
import ru.hh.jclient.common.util.storage.StorageUtils;
import ru.hh.jclient.common.util.storage.StorageUtils.Storages;
import ru.hh.jclient.common.util.storage.ThreadLocalStorage;

/**
 * Creates and removes {@link HttpClientContext} using ThreadLocal to store it.
 */
public class HttpClientContextThreadLocalSupplier extends ThreadLocalStorage<HttpClientContext> {

  private final List<Supplier<HttpClientEventListener>> eventListenerSuppliers;
  private final Storages storagesForTransfer;

  public HttpClientContextThreadLocalSupplier() {
    this.storagesForTransfer = StorageUtils.build(Set.of());
    this.eventListenerSuppliers = new CopyOnWriteArrayList<>();
  }

  public HttpClientContextThreadLocalSupplier(Supplier<HttpClientContext> valueSupplier) {
    this(valueSupplier, true);
  }

  /**
   * Constructs HttpClientContextThreadLocalSupplier.
   *
   * @param valueSupplier supplier for client context value
   * @param useThreadLocal use specified value supplier as initial for thread local storage
   */
  public HttpClientContextThreadLocalSupplier(Supplier<HttpClientContext> valueSupplier, boolean useThreadLocal) {
    super(valueSupplier, useThreadLocal);
    this.storagesForTransfer = StorageUtils.build(Set.of());
    this.eventListenerSuppliers = new CopyOnWriteArrayList<>();
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
   * Add event listener supplier to be used in all contexts.
   *
   * @param eventListenerSupplier supplier to add
   * @return this instance
   */
  public HttpClientContextThreadLocalSupplier registerEventListenerSupplier(Supplier<HttpClientEventListener> eventListenerSupplier) {
    this.eventListenerSuppliers.add(eventListenerSupplier);
    return this;
  }

  /**
   * Add event listener supplier to client context of current thread. Won't do anything if context is not set yet.
   *
   * @param eventListenerSupplier supplier to add
   * @return this instance
   */
  public HttpClientContextThreadLocalSupplier registerEventListenerSupplierForCurrentContext(
      Supplier<HttpClientEventListener> eventListenerSupplier) {
    ofNullable(get()).ifPresent(context -> context.withEventListenerSupplier(eventListenerSupplier));
    return this;
  }

  /**
   * Remove event listener supplier from client context of current thread. Won't do anything if context is not set yet or
   * if supplier wasn't added previously.
   *
   * @param eventListenerSupplier supplier to remove
   * @return this instance
   */
  public HttpClientContextThreadLocalSupplier removeEventListenerSupplierFromCurrentContext(
      Supplier<HttpClientEventListener> eventListenerSupplier) {
    ofNullable(get()).ifPresent(context -> context.removeEventListenerSupplier(eventListenerSupplier));
    return this;
  }

  /**
   * Creates and stores new context using provided headers. This method should be called before any invocations of jclient implementation in the
   * current thread.
   */
  public void addContext(Map<String, List<String>> headers, Map<String, List<String>> queryParams) {
    set(new HttpClientContext(headers, queryParams, eventListenerSuppliers, storagesForTransfer));
  }

  public ContextBuilder forCurrentThread() {
    return new ContextBuilder().withEventListenerSuppliers(eventListenerSuppliers);
  }

  public class ContextBuilder {
    private final Map<String, List<String>> headers = new HashMap<>();
    private final Map<String, List<String>> queryParams = new HashMap<>();
    private final List<Supplier<HttpClientEventListener>> eventListenerSuppliers = new ArrayList<>();

    private HttpClientContext previousContext;

    public ContextBuilder withHeaders(Map<String, List<String>> headers) {
      this.headers.putAll(headers);
      return this;
    }

    public ContextBuilder withQueryParams(Map<String, List<String>> queryParams) {
      this.queryParams.putAll(queryParams);
      return this;
    }

    public ContextBuilder withRequestId(String requestId) {
      if (requestId != null) {
        this.headers.put(X_REQUEST_ID, List.of(requestId));
      }
      return this;
    }

    public ContextBuilder withEventListenerSuppliers(List<Supplier<HttpClientEventListener>> eventListenerSuppliers) {
      this.eventListenerSuppliers.addAll(eventListenerSuppliers);
      return this;
    }

    public void execute(Runnable runnable) {
      execute(() -> {
        runnable.run();
        return null;
      });
    }

    public <E extends Throwable> void executeFailable(FailableRunnable<E> runnable) throws E {
      executeFailable(() -> {
        runnable.run();
        return null;
      });
    }

    public <T> T execute(Supplier<T> supplier) {
      return executeFailable(supplier::get);
    }

    public <T, E extends Throwable> T executeFailable(FailableSupplier<T, E> supplier) throws E {
      prepareContext();
      try {
        return supplier.get();
      } finally {
        resetContext();
      }
    }

    private void prepareContext() {
      previousContext = get();
      set(new HttpClientContext(headers, queryParams, eventListenerSuppliers, storagesForTransfer));
    }

    private void resetContext() {
      if (previousContext == null) {
        clear();
      } else {
        set(previousContext);
      }
    }
  }
}
