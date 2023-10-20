package ru.hh.jclient.common;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class JClientConfigurer<T extends ConfigurableJClientBase<T>> {

  private T client;

  public JClientConfigurer(T client) {
    this.client = client;
  }

  public JClientConfigurer<T> headers(Map<String, List<String>> headers) {
    return this;
  }

  public JClientConfigurer<T> header(String name, String value) {
    return this;
  }

  public JClientConfigurer<T> requestId(String value) {
    return this;
  }

  public JClientConfigurer<T> queryParams(Map<String, List<String>> headers) {
    return this;
  }

  public JClientConfigurer<T> queryParam(String name, String value) {
    return this;
  }

  public JClientConfigurer<T> debugSupplier(Supplier<RequestDebug> supplier) {
    return this;
  }

  public T configured() {
    return client;
  }

}
