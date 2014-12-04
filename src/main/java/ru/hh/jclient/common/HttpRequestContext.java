package ru.hh.jclient.common;

import static ru.hh.jclient.common.HttpClient.HEADER_DEBUG;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.google.common.collect.HashMultimap;

public class HttpRequestContext {

  private HashMultimap<String, String> headers = HashMultimap.create();
  private boolean debugMode;

  public void setHeaders(HashMultimap<String, String> headers) {
    this.headers.putAll(headers);
  }

  public void setHeaders(Map<String, List<String>> headers) {
    for (Entry<String, List<String>> entry : headers.entrySet()) {
      this.headers.putAll(entry.getKey(), entry.getValue());
    }
    if (headers.containsKey(HEADER_DEBUG) && headers.get(HEADER_DEBUG).size() == 1 && headers.get(HEADER_DEBUG).get(0).toLowerCase() == "true") {
      this.debugMode = true;
    }
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
