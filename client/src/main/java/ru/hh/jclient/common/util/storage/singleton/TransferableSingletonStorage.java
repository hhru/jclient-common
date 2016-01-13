package ru.hh.jclient.common.util.storage.singleton;

import ru.hh.jclient.common.util.storage.TransferableStorage;

public class TransferableSingletonStorage<T> implements TransferableStorage<T> {

  private T value;

  public TransferableSingletonStorage(T value) {
    this.value = value;
  }

  public TransferableSingletonStorage() {
  }

  @Override
  public T get() {
    return value;
  }

  @Override
  public void set(T t) {
    this.value = t;
  }

  @Override
  public void remove() {
    this.value = null;
  }

}
