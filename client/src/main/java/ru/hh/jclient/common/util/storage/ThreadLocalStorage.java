package ru.hh.jclient.common.util.storage;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadLocalStorage<T> implements Storage<T> {

  private static final Logger LOG = LoggerFactory.getLogger(ThreadLocalStorage.class);

  // can't be static - need instance of storage per instance of this class
  private final ThreadLocal<T> storage;

  public ThreadLocalStorage(Supplier<T> initialValueSupplier) {
    storage = ThreadLocal.withInitial(initialValueSupplier);

  }

  public ThreadLocalStorage() {
    storage = ThreadLocal.withInitial(() -> null);
  }

  @Override
  public T get() {
    return storage.get();
  }

  @Override
  public void set(T t) {
    storage.set(t);
  }

  @Override
  public void clear() {
    storage.remove();
  }

  @Override
  public Transfer prepareTransferToAnotherThread() {
    return new PreparedValueTransfer<T>(get(), this);
  }

  public static class PreparedValueTransfer<T> implements Transfer {

    private T valueForTransfer;
    private Storage<T> parent;

    private PreparedValueTransfer(T value, Storage<T> parent) {
      this.valueForTransfer = value;
      this.parent = parent;
    }

    @Override
    public void perform() {
      T currentValue = parent.get();
      if (currentValue != null) {
        LOG.warn("[{}] Replacing existing object {} with {}", parent.getClass().getName(), currentValue, valueForTransfer);
      }
      parent.set(valueForTransfer);
    }

    @Override
    public void rollback() {
      T currentValue = parent.get();
      if (currentValue != valueForTransfer) {
        LOG.warn("[{}] Unexpected object when removing {} - was {}", parent.getClass().getName(), valueForTransfer, currentValue);
      }
      else if (currentValue == null) {
        LOG.warn("[{}] Unexpected object when removing {} - null", parent.getClass().getName(), valueForTransfer);
      }
      parent.clear();
      valueForTransfer = null;
    }

  }

}
