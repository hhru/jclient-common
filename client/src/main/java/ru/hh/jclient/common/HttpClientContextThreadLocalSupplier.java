package ru.hh.jclient.common;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import org.slf4j.MDC;
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

  private final List<Supplier<RequestDebug>> requestDebugSuppliers;
  private final Storages storagesForTransfer;

  public HttpClientContextThreadLocalSupplier() {
    this.storagesForTransfer = StorageUtils.build(Set.of());
    this.requestDebugSuppliers = new CopyOnWriteArrayList<>();
  }

  public HttpClientContextThreadLocalSupplier(Supplier<HttpClientContext> initialValueSupplier) {
    super(initialValueSupplier);
    this.storagesForTransfer = StorageUtils.build(Set.of());
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

  public ContextBuilder forCurrentThread() {
    return new ContextBuilder();
  }

  public class ContextBuilder {
    private final Map<String, List<String>> headers = new HashMap<>();
    private final Map<String, List<String>> queryParams = new HashMap<>();
    private final Map<String, List<String>> passQueryParams = new HashMap<>();
    private final List<Supplier<RequestDebug>> debugSuppliers = new ArrayList<>();

    private HttpClientContext previousContext;
    private String previousRequestId;

    public ContextBuilder() {
      String requestId = MDC.get("rid");
      if (requestId != null) {
        this.headers.put(X_REQUEST_ID, List.of(requestId));
      }
    }

    public ContextBuilder withHeaders(Map<String, List<String>> headers) {
      this.headers.putAll(headers);
      return this;
    }

    public ContextBuilder withQueryParams(Map<String, List<String>> queryParams) {
      this.queryParams.putAll(queryParams);
      return this;
    }

    public ContextBuilder withPassQueryParams(Map<String, List<String>> passQueryParams) {
      this.passQueryParams.putAll(passQueryParams);
      return this;
    }

    public ContextBuilder withRequestId(String requestId) {
      if (requestId != null) {
        this.headers.put(X_REQUEST_ID, List.of(requestId));
      }
      return this;
    }

    public ContextBuilder withDebugSuppliers(List<Supplier<RequestDebug>> debugSuppliers) {
      this.debugSuppliers.addAll(debugSuppliers);
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
      previousRequestId = Optional.ofNullable(previousContext)
        .map(HttpClientContext::getHeaders)
        .flatMap(RequestUtils::getRequestId)
        .orElseGet(() -> MDC.get("rid"));

      Optional<String> requestId = RequestUtils.getRequestId(headers);
      set(new HttpClientContext(LocalDateTime.now(), headers, queryParams, passQueryParams, debugSuppliers, storagesForTransfer));
      requestId.ifPresent(s -> MDC.put("rid", s));
    }

    private void resetContext() {
      if (previousContext == null) {
        clear();
      } else {
        set(previousContext);
      }
      if (previousRequestId == null) {
        MDC.remove("rid");
      } else {
        MDC.put("rid", previousRequestId);
      }
    }
  }
}
