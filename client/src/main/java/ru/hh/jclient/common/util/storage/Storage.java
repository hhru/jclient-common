package ru.hh.jclient.common.util.storage;

import java.util.function.Supplier;

public interface Storage<T> extends Supplier<T> {

  @Override
  T get();

  void set(T t);

  void clear();

  Transfer prepareTransferToAnotherThread();

}
