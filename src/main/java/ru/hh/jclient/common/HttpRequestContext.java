package ru.hh.jclient.common;

import static ru.hh.jclient.common.RequestUtils.isInDebugMode;
import java.util.List;
import java.util.Map;
import com.google.common.collect.HashMultimap;

public class HttpRequestContext {

  private HashMultimap<String, String> headers = HashMultimap.create();
  private boolean debugMode;

  public void setHeaders(HashMultimap<String, String> headers) {
    this.headers.putAll(headers);
  }

  public void setHeaders(Map<String, List<String>> headers) {
    headers.entrySet().forEach(e -> this.headers.putAll(e.getKey(), e.getValue()));
    this.debugMode = isInDebugMode(headers);
  }

  public HashMultimap<String, String> getHeaders() {
    return headers;
  }

  public void setDebugMode(boolean debugMode) {
    this.debugMode = debugMode;
  }

  public boolean isDebugMode() {
    return this.debugMode;
  }
}
