package ru.hh.jclient.common;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import ru.hh.jclient.common.util.jersey.UriBuilder;

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

  protected String url(String resourceMethodPath) {
    return host + resourceMethodPath;
  }

  protected String jerseyUrl(String resourceMethodPath, Object... pathParams) {
    return UriBuilder.fromPath(url(resourceMethodPath)).build(pathParams).toString();
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
