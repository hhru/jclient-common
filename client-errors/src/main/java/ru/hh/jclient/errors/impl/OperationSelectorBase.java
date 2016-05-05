package ru.hh.jclient.errors.impl;

import java.util.function.Supplier;

public class OperationSelectorBase {

  protected Supplier<String> errorMessage;

  public OperationSelectorBase(String errorMessage, Object... params) {
    if (params == null || params.length == 0) {
      this.errorMessage = () -> errorMessage;
    }
    else {
      this.errorMessage = () -> String.format(errorMessage, params);
    }
  }

}
