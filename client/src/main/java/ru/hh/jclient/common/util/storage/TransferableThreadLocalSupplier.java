package ru.hh.jclient.common.util.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferableThreadLocalSupplier<T> extends AbstractTransferableStoredSupplier<T> {

  private static final Logger LOG = LoggerFactory.getLogger(TransferableThreadLocalSupplier.class);

  // can't be static - need instance of storage per instance of this class
  private final ThreadLocal<T> STORAGE = ThreadLocal.withInitial(() -> null);

  @Override
  public T get() {
    return STORAGE.get();
  }

  @Override
  public void set(T t) {
    STORAGE.set(t);
  }

  @Override
  public void remove() {
    STORAGE.remove();
  }

  @Override
  protected Logger getLogger() {
    return LOG;
  }
}
