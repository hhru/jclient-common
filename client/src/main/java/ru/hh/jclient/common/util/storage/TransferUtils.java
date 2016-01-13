package ru.hh.jclient.common.util.storage;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TransferUtils {

  private TransferUtils() {
  }

  public static Transfers build(Collection<TransferableSupplier<?>> suppliers) {
    return new Transfers(suppliers);
  }

  public static Transfers build(TransferableSupplier<?> supplier) {
    return new Transfers(Collections.singleton(supplier));
  }

  public static Transfers build(TransferableSupplier<?>... suppliers) {
    return new Transfers(Arrays.asList(suppliers));
  }

  public static class PreparedTransfers {

    protected List<Transfer<?>> transfers;

    protected PreparedTransfers(Collection<TransferableSupplier<?>> suppliers) {
      transfers = suppliers.stream().map(s -> new Transfer<>(s)).collect(Collectors.toList());
    }

    public void perform() {
      transfers.forEach(Transfer::perform);
    }

    public void rollback() {
      transfers.forEach(Transfer::rollback);
    }
  }

  public static class Transfers extends PreparedTransfers {

    protected Transfers(Collection<TransferableSupplier<?>> suppliers) {
      super(suppliers);
    }

    public void add(TransferableSupplier<?> supplier) {
      transfers.add(new Transfer<>(supplier));
    }

    public PreparedTransfers prepare() {
      transfers.forEach(Transfer::prepare);
      return this;
    }

  }

  private static class Transfer<T> {
    public TransferableSupplier<T> supplier;
    public T value;

    public Transfer(TransferableSupplier<T> supplier) {
      this.supplier = supplier;
    }

    public void prepare() {
      value = supplier.get();
    }

    public void perform() {
      supplier.set(value);
    }

    public void rollback() {
      supplier.remove(value);
    }
  }
}
