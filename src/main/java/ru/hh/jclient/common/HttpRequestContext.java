package ru.hh.jclient.common;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.google.common.collect.HashMultimap;

public class HttpRequestContext {

  private HashMultimap<String, String> headers = HashMultimap.create();

  public void setHeaders(HashMultimap<String, String> headers) {
    this.headers.putAll(headers);
  }

  public void setHeaders(Map<String, List<String>> headers) {
    for (Entry<String, List<String>> entry : headers.entrySet()) {
      headers.put(entry.getKey(), entry.getValue());
    }
  }

  public HashMultimap<String, String> getHeaders() {
    return headers;
  }
}
