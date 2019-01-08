package ru.hh.jclient.common;

import ru.hh.jclient.common.util.storage.Storage;

import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public abstract class JClientBase {

  public static final String HTTP_GET = "GET";
  public static final String HTTP_POST = "POST";
  public static final String HTTP_PUT = "PUT";
  public static final String HTTP_DELETE = "DELETE";

  protected String host;
  protected HttpClientBuilder http;

  protected JClientBase(String host, HttpClientBuilder http) {
    this.host = requireNotEmpty(host, "host");
    this.http = requireNotNull(http, "http");
  }

  protected JClientBase(String host, String path, HttpClientBuilder http) {
    this(requireNotEmpty(host, "host") + ofNullable(path).orElse(""), http);
  }

  public static <T> T executeInContext(JClientBase client, HttpClientContext context, Callable<T> action) throws Exception {
    Storage<HttpClientContext> contextSupplier = client.http.getContextSupplier();
    HttpClientContext initialContext = contextSupplier.get();
    contextSupplier.set(context);
    try {
      return action.call();
    } finally {
      contextSupplier.set(initialContext);
    }
  }

  protected String url(String resourceMethodPath) {
    return host + resourceMethodPath;
  }

  /**
   * This method requires jsr311-api (jersey v1) or javax.ws.rs-api (jersey v2) to present in your dependencies.
   * Otherwise the method throws {@link UnsupportedOperationException}.
   *
   * @throws UnsupportedOperationException if javax.ws.rs.core.UriBuilder is not in the classpath.
   */
  protected String jerseyUrl(String resourceMethodPath, Object... pathParams) {
    try {
      Class.forName("javax.ws.rs.core.UriBuilder");
      return javax.ws.rs.core.UriBuilder.fromPath(url(resourceMethodPath)).build(pathParams).toString();
    } catch (ClassNotFoundException e) {
      throw new UnsupportedOperationException("Unable to find javax.ws.rs.core.UriBuilder in classpath");
    }
  }

  protected RequestBuilder get(String url, Object... queryParams) {
    return build(HTTP_GET, url, queryParams);
  }

  protected RequestBuilder post(String url, Object... queryParams) {
    return build(HTTP_POST, url, queryParams);
  }

  protected RequestBuilder put(String url, Object... queryParams) {
    return build(HTTP_PUT, url, queryParams);
  }

  protected RequestBuilder delete(String url, Object... queryParams) {
    return build(HTTP_DELETE, url, queryParams);
  }

  protected RequestBuilder build(String method, String url, Object... queryParams) {
    RequestBuilder builder = new RequestBuilder(method).setUrl(url);
    if (queryParams == null) {
      return builder;
    }
    checkArgument(queryParams.length % 2 == 0, "params size must be even");
    for (int i = 0; i < queryParams.length; i += 2) {
      builder.addQueryParam(queryParams[i].toString(), queryParams[i + 1].toString());
    }
    return builder;
  }
  
  protected static String requireNotEmpty(String arg, String argName) {
    checkArgument(arg != null && !arg.isEmpty(), "%s is null or empty string", argName);
    return arg;
  }
  
  protected static <T> T requireNotNull(T arg, String argName) {
    return requireNonNull(arg, () -> argName + "is null");
  }
}
