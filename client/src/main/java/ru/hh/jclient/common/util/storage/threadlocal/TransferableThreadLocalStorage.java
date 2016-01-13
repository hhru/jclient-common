package ru.hh.jclient.common.util.storage.threadlocal;

import ru.hh.jclient.common.util.storage.TransferableStorage;

public class TransferableThreadLocalStorage<T> implements TransferableStorage<T> {

  private final ThreadLocal<?> STORAGE = ThreadLocal.withInitial(() -> null);

  @SuppressWarnings("unchecked")
  private ThreadLocal<T> storage() {
    return (ThreadLocal<T>) STORAGE;
  }

  @Override
  public T get() {
    return storage().get();
  }

  @Override
  public void set(T t) {
    storage().set(t);
  }

  @Override
  public void remove() {
    storage().remove();
  }
}
