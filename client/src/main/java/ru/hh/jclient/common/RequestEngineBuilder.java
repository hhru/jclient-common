package ru.hh.jclient.common;

public interface RequestEngineBuilder<T extends RequestEngine> {
  T build(Request request, RequestingStrategy.RequestExecutor executor);
  HttpClient backToClient();
}
