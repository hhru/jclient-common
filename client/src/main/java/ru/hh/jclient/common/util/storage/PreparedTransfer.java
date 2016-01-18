package ru.hh.jclient.common.util.storage;

public interface PreparedTransfer {

  void performTransfer();

  void rollbackTransfer();

}
