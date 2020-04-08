package ru.hh.jclient.common;

public interface RequestEngineBuilder {
  RequestEngine build(Request request, RequestStrategy.RequestExecutor executor);
  HttpClient backToClient();
}
