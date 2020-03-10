package ru.hh.jclient.common;

public interface RequestEngineBuilder<T extends RequestEngine> {
  T build(Request request, RequestStrategy.RequestExecutor executor);
  HttpClient backToClient();
}
