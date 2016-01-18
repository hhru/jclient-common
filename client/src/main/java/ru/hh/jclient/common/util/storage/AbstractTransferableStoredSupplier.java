package ru.hh.jclient.common.util.storage;

import org.slf4j.Logger;

public abstract class AbstractTransferableStoredSupplier<T> implements TransferableSupplier<T> {

  protected abstract void set(T t);

  protected abstract void remove();

  protected abstract Logger getLogger();

  @Override
  public PreparedTransfer prepareTransfer() {
    return new PreparedValueTransfer<T>(get(), this);
  }

  public static class PreparedValueTransfer<T> implements PreparedTransfer {

    private T valueForTransfer;
    private AbstractTransferableStoredSupplier<T> parent;

    private PreparedValueTransfer(T value, AbstractTransferableStoredSupplier<T> parent) {
      this.valueForTransfer = value;
      this.parent = parent;
    }

    @Override
    public void performTransfer() {
      T currentValue = parent.get();
      if (currentValue != null) {
        parent.getLogger().warn("Replacing existing object {} with {}", currentValue, valueForTransfer);
      }
      parent.set(valueForTransfer);
    }

    @Override
    public void rollbackTransfer() {
      T currentValue = parent.get();
      if (currentValue != valueForTransfer) {
        parent.getLogger().warn("Unexpected object when removing {} - was {}", valueForTransfer, currentValue);
      }
      else if (currentValue == null) {
        parent.getLogger().warn("Unexpected object when removing {} - null", valueForTransfer);
      }
      parent.remove();
      valueForTransfer = null;
    }

  }

}
