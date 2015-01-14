package ru.hh.jclient.common;

import static ru.hh.jclient.common.RequestUtils.isInDebugMode;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.google.common.collect.HashMultimap;

public class HttpClientContext {

  private HashMultimap<String, String> headers = HashMultimap.create();
  private boolean debugMode;
  private Supplier<RequestDebug> debugSupplier;

  public HttpClientContext(Map<String, List<String>> headers, Supplier<RequestDebug> debugSupplier) {
    headers.entrySet().forEach(e -> this.headers.putAll(e.getKey(), e.getValue()));
    this.debugMode = isInDebugMode(headers);
    this.debugSupplier = debugSupplier;
  }

  public HashMultimap<String, String> getHeaders() {
    return headers;
  }

  public boolean isDebugMode() {
    return this.debugMode;
  }

  public Supplier<RequestDebug> getDebugSupplier() {
    return debugSupplier;
  }
}
