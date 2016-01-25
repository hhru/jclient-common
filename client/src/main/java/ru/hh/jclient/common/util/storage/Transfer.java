package ru.hh.jclient.common.util.storage;

public interface Transfer {

  void perform();

  void rollback();

}
