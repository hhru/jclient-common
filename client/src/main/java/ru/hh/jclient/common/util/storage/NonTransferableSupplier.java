package ru.hh.jclient.common.util.storage;

import java.util.function.Supplier;

public class NonTransferableSupplier<T> implements TransferableSupplier<T> {

  private Supplier<T> supplier;

  public NonTransferableSupplier(Supplier<T> supplier) {
    this.supplier = supplier;
  }

  @Override
  public T get() {
    return supplier.get();
  }

  @Override
  public PreparedTransfer prepareTransfer() {
    return DO_NOTHING;
  }

  private static final PreparedTransfer DO_NOTHING = new PreparedTransfer() {

    @Override
    public void performTransfer() {
      // do nothing
    }

    @Override
    public void rollbackTransfer() {
      // do nothing
    }
  };

}
