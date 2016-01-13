package ru.hh.jclient.common.util.storage;

public interface TransferableStorage<T> {

  T get();

  void set(T t);

  void remove();

}
