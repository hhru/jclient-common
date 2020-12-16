package ru.hh.jclient.common;

public interface RequestEngineBuilder<REB extends RequestEngineBuilder<REB>> {
  RequestEngine build(Request request, RequestStrategy.RequestExecutor executor);
  REB withTimeoutMultiplier(Double timeoutMultiplier);
  HttpClient backToClient();
}
