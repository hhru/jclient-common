package ru.hh.jclient.common.util.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

    private Collection<PreparedTransfer> transfers;

    protected PreparedTransfers(Collection<PreparedTransfer> transfers) {
      this.transfers = transfers;
    }

    public void perform() {
      transfers.forEach(PreparedTransfer::performTransfer);
    }

    public void rollback() {
      transfers.forEach(PreparedTransfer::rollbackTransfer);
    }
  }

  public static class Transfers {

    protected Collection<TransferableSupplier<?>> suppliers;

    protected Transfers(Collection<TransferableSupplier<?>> suppliers) {
      this.suppliers = new ArrayList<>(suppliers);
    }

    public void add(TransferableSupplier<?> supplier) {
      this.suppliers.add(supplier);
    }

    public PreparedTransfers prepare() {
      return new PreparedTransfers(suppliers.stream().map(TransferableSupplier::prepareTransfer).collect(Collectors.toList()));
    }

  }
}
