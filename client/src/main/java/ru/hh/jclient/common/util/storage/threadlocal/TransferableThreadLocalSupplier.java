package ru.hh.jclient.common.util.storage.threadlocal;

import ru.hh.jclient.common.util.storage.TransferableStorage;
import ru.hh.jclient.common.util.storage.TransferableSupplier;

public class TransferableThreadLocalSupplier<T> implements TransferableSupplier<T> {

  private TransferableStorage<T> storage = new TransferableThreadLocalStorage<>();

  @Override
  public TransferableStorage<T> getStorage() {
    return storage;
  }

}
