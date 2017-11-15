package ru.hh.jclient.common;

import java.util.Objects;

public class Param {

  private final com.ning.http.client.Param delegate;

  Param(com.ning.http.client.Param param) {
    this.delegate = param;
  }

  public Param(String name, String value) {
    this.delegate = new com.ning.http.client.Param(name, value);
  }

  public String getName() {
    return delegate.getName();
  }

  public String getValue() {
    return delegate.getValue();
  }

  @Override
  public boolean equals(Object obj) {
    return Objects.equals(obj, this.delegate);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  com.ning.http.client.Param getDelegate() {
    return delegate;
  }

}
