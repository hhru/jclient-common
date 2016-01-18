package ru.hh.jclient.common.util.storage;

import org.slf4j.MDC;
import ru.hh.jclient.common.util.MDCCopy;

public class TransferableMDCSupplier implements TransferableSupplier<MDCCopy> {

  @Override
  public MDCCopy get() {
    return MDCCopy.capture();
  }

  @Override
  public PreparedTransfer prepareTransfer() {
    return new PreparedMDCTransfer();
  }

  public static class PreparedMDCTransfer implements PreparedTransfer {

    private MDCCopy mdc;

    private PreparedMDCTransfer() {
      this.mdc = MDCCopy.capture();
    }

    @Override
    public void performTransfer() {
      mdc.restore();
    }

    @Override
    public void rollbackTransfer() {
      MDC.clear();
    }
  }

}
