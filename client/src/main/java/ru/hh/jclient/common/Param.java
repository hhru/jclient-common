package ru.hh.jclient.common;

import java.util.Objects;

public class Param {

  private final org.asynchttpclient.Param delegate;

  Param(org.asynchttpclient.Param param) {
    this.delegate = param;
  }

  public Param(String name, String value) {
    this.delegate = new org.asynchttpclient.Param(name, value);
  }

  public static Param of(Object name, Object value) {
    return new Param(name == null ? null : name.toString(), value == null ? null : value.toString());
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

  org.asynchttpclient.Param getDelegate() {
    return delegate;
  }

  @Override
  public String toString() {
    return "Param{" +
        getName() + "=" + getValue() +
        '}';
  }
}
