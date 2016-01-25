package ru.hh.jclient.common.util.storage;

import org.slf4j.MDC;
import ru.hh.jclient.common.util.MDCCopy;

public class MDCStorage implements Storage<MDCCopy> {

  @Override
  public MDCCopy get() {
    return MDCCopy.capture();
  }

  @Override
  public void set(MDCCopy t) {
    t.restore();
  }

  @Override
  public void clear() {
    MDC.clear();
  }

  @Override
  public Transfer prepareTransferToAnotherThread() {
    return new PreparedMDCTransfer();
  }

  public static class PreparedMDCTransfer implements Transfer {

    private MDCCopy mdc;

    private PreparedMDCTransfer() {
      this.mdc = MDCCopy.capture();
    }

    @Override
    public void perform() {
      mdc.restore();
    }

    @Override
    public void rollback() {
      MDC.clear();
    }
  }

}
