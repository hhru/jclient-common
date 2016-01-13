package ru.hh.jclient.common.util.storage.singleton;

import ru.hh.jclient.common.util.storage.TransferableStorage;
import ru.hh.jclient.common.util.storage.TransferableSupplier;

public class TransferableSingletonSupplier<T> implements TransferableSupplier<T> {

  private TransferableSingletonStorage<T> storage = new TransferableSingletonStorage<>();

  @Override
  public TransferableStorage<T> getStorage() {
    return storage;
  }

}
