package ru.hh.jclient.common;

import java.util.Map;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import ru.hh.jclient.common.util.storage.Storage;

public abstract class JClientBase {

  public static final String HTTP_GET = "GET";
  public static final String HTTP_POST = "POST";
  public static final String HTTP_PUT = "PUT";
  public static final String HTTP_DELETE = "DELETE";

  protected String host;
  protected HttpClientFactory http;

  protected JClientBase(String host, HttpClientFactory http) {
    this.host = requireNotEmpty(host, "host");
    this.http = requireNotNull(http, "http");
  }

  protected JClientBase(String host, String path, HttpClientFactory http) {
    this(requireNotEmpty(host, "host") + ofNullable(path).orElse(""), http);
  }

  protected String url(String resourceMethodPath) {
    return host + resourceMethodPath;
  }

  /**
   * This method requires jsr311-api (jersey v1) or jakarta.ws.rs-api (jersey v2) to present in your dependencies.
   * Otherwise the method throws {@link UnsupportedOperationException}.
   *
   * @throws UnsupportedOperationException if jakarta.ws.rs.core.UriBuilder is not in the classpath.
   */
  protected String jerseyUrl(String resourceMethodPath, Object... pathParams) {
    try {
      Class.forName("jakarta.ws.rs.core.UriBuilder");
      return jakarta.ws.rs.core.UriBuilder.fromPath(url(resourceMethodPath)).build(pathParams).toString();
    } catch (ClassNotFoundException e) {
      throw new UnsupportedOperationException("Unable to find jakarta.ws.rs.core.UriBuilder in classpath");
    }
  }

  /**
   * prepare RequestBuilder instance for GET method
   * @param url path to request from
   * @param queryParams flat array of pairs {queryParamName, queryParamValue}
   * @return RequestBuilder instance
   */
  protected RequestBuilder get(String url, Object... queryParams) {
    return build(HTTP_GET, url, queryParams);
  }

  protected RequestBuilder get(String url, Map<String, Object> queryParams) {
    return build(HTTP_GET, url, queryParams);
  }

  /**
   * prepare RequestBuilder instance for POST method
   * @param url path to request from
   * @param queryParams flat array of pairs {queryParamName, queryParamValue}
   * @return RequestBuilder instance
   */
  protected RequestBuilder post(String url, Object... queryParams) {
    return build(HTTP_POST, url, queryParams);
  }

  protected RequestBuilder post(String url, Map<String, Object> queryParams) {
    return build(HTTP_POST, url, queryParams);
  }

  /**
   * prepare RequestBuilder instance for PUT method
   * @param url path to request from
   * @param queryParams flat array of pairs {queryParamName, queryParamValue}
   * @return RequestBuilder instance
   */
  protected RequestBuilder put(String url, Object... queryParams) {
    return build(HTTP_PUT, url, queryParams);
  }

  protected RequestBuilder put(String url, Map<String, Object> queryParams) {
    return build(HTTP_PUT, url, queryParams);
  }

  /**
   * prepare RequestBuilder instance for DELETE method
   * @param url path to request from
   * @param queryParams flat array of pairs {queryParamName, queryParamValue}
   * @return RequestBuilder instance
   */
  protected RequestBuilder delete(String url, Object... queryParams) {
    return build(HTTP_DELETE, url, queryParams);
  }

  protected RequestBuilder delete(String url, Map<String, Object> queryParams) {
    return build(HTTP_DELETE, url, queryParams);
  }

  /**
   * prepare RequestBuilder instance
   * @param method http method
   * @param url path to request from
   * @param queryParams flat array of pairs {queryParamName, queryParamValue}
   * @return RequestBuilder instance
   */
  protected RequestBuilder build(String method, String url, Object... queryParams) {
    RequestBuilder builder = new RequestBuilder(method).setUrl(url);
    if (queryParams == null) {
      return builder;
    }
    if (queryParams.length % 2 != 0) {
      throw new IllegalArgumentException("params size must be even");
    }
    for (int i = 0; i < queryParams.length; i += 2) {
      //only string keys allowed
      builder.addQueryParam((String) queryParams[i], queryParams[i + 1].toString());
    }
    return builder;
  }

  protected RequestBuilder build(String method, String url, Map<String, Object> queryParams) {
    RequestBuilder builder = new RequestBuilder(method).setUrl(url);
    if (queryParams == null) {
      return builder;
    }
    builder.addQueryParamMap(queryParams);
    return builder;
  }

  protected Storage<HttpClientContext> getContextSupplier() {
    return http.getContextSupplier();
  }
  
  protected static String requireNotEmpty(String arg, String argName) {
    if (arg == null || arg.isEmpty()) {
      throw new IllegalArgumentException(argName + " is null or empty string");
    }
    return arg;
  }
  
  protected static <T> T requireNotNull(T arg, String argName) {
    return requireNonNull(arg, () -> argName + " is null");
  }
}
