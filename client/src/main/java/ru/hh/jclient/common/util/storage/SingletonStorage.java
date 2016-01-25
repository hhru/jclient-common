package ru.hh.jclient.common.util.storage;

import java.util.function.Supplier;

public class SingletonStorage<T> implements Storage<T> {

  private Supplier<T> supplier;

  public SingletonStorage(Supplier<T> supplier) {
    this.supplier = supplier;
  }

  public SingletonStorage(T value) {
    this(() -> value);
  }

  @Override
  public T get() {
    return supplier.get();
  }

  @SuppressWarnings("unused")
  @Override
  public void set(T t) {
    // do nothing
  }

  @Override
  public void clear() {
    // do nothing
  }

  @Override
  public Transfer prepareTransferToAnotherThread() {
    return DO_NOTHING;
  }

  private static Transfer DO_NOTHING = new Transfer() {

    @Override
    public void perform() {
      // do nothing
    }

    @Override
    public void rollback() {
      // do nothing
    }

  };

}
