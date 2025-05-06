package ru.hh.jclient.common.util.storage;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadLocalStorage<T> implements Storage<T> {

  private static final Logger LOG = LoggerFactory.getLogger(ThreadLocalStorage.class);

  // can't be static - need instance of storage per instance of this class
  private final ThreadLocal<T> storage;
  private final Supplier<T> valueSupplier;
  private final boolean hasInitValue;

  public ThreadLocalStorage() {
    this(() -> null, true, false);
  }

  public ThreadLocalStorage(Supplier<T> valueSupplier) {
    this(valueSupplier, true, true);
  }

  public ThreadLocalStorage(Supplier<T> valueSupplier, boolean useThreadLocal) {
    this(valueSupplier, useThreadLocal, true);
  }

  private ThreadLocalStorage(Supplier<T> valueSupplier, boolean useThreadLocal, boolean hasInitValue) {
    this.hasInitValue = hasInitValue;
    if (useThreadLocal) {
      storage = ThreadLocal.withInitial(valueSupplier);
      this.valueSupplier = null;
    } else {
      storage = null;
      this.valueSupplier = valueSupplier;
    }
  }

  @Override
  public T get() {
    return storage == null ? valueSupplier.get() : storage.get();
  }

  @Override
  public void set(T t) {
    if (storage != null) {
      storage.set(t);
    }
  }

  @Override
  public void clear() {
    if (storage != null) {
      storage.remove();
    }
  }

  @Override
  public Transfer prepareTransferToAnotherThread() {
    if (storage == null) {
      return null;
    }
    return new PreparedValueTransfer<>(get(), this, hasInitValue);
  }

  public static class PreparedValueTransfer<T> implements Transfer {

    private final boolean parentHasInitValue;
    private T valueForTransfer;
    private final Storage<T> parent;

    private PreparedValueTransfer(T value, Storage<T> parent, boolean parentHasInitValue) {
      this.parentHasInitValue = parentHasInitValue;
      this.valueForTransfer = value;
      this.parent = parent;
    }

    @Override
    public void perform() {
      if (!parentHasInitValue) {
        T currentValue = parent.get();
        if (currentValue != null) {
          LOG.warn("[{}] Replacing existing object {} with {}", parent.getClass().getName(), currentValue, valueForTransfer);
        }
      }
      parent.set(valueForTransfer);
    }

    @Override
    public void rollback() {
      T currentValue = parent.get();
      if (currentValue != valueForTransfer) {
        LOG.warn("[{}] Unexpected object when removing {} - was {}", parent.getClass().getName(), valueForTransfer, currentValue);
      }
      parent.clear();
      valueForTransfer = null;
    }

  }

}
