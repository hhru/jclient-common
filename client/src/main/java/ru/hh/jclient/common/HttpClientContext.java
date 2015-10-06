package ru.hh.jclient.common;

import static java.util.Objects.requireNonNull;
import static ru.hh.jclient.common.RequestUtils.isInDebugMode;
import static ru.hh.jclient.common.util.MoreCollectors.toFluentCaseInsensitiveStringsMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;

/**
 * Context of global (incoming) request, that is going to spawn local (outgoing) request using
 * {@link HttpClientBuilder#with(com.ning.http.client.Request)}.
 */
public class HttpClientContext {

  private FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();
  private boolean debugMode;
  private Supplier<RequestDebug> debugSupplier;

  /**
   * Creates context.
   *
   * @param headers headers of global request. Some of them can be used by the local request.
   * @param queryParams query params of global request
   * @param debugSupplier supplier of object used to gather debug information
   */
  public HttpClientContext(Map<String, List<String>> headers, Map<String, List<String>> queryParams, Supplier<RequestDebug> debugSupplier) {
    this.headers = requireNonNull(headers, "headers must not be null")
        .entrySet()
        .stream()
        .collect(toFluentCaseInsensitiveStringsMap(Entry::getKey, Entry::getValue));
    this.debugMode = isInDebugMode(headers, queryParams);
    this.debugSupplier = requireNonNull(debugSupplier, "debugSupplier must not be null");
  }

  public FluentCaseInsensitiveStringsMap getHeaders() {
    return headers;
  }

  public boolean isDebugMode() {
    return this.debugMode;
  }

  public Supplier<RequestDebug> getDebugSupplier() {
    return debugSupplier;
  }
}
