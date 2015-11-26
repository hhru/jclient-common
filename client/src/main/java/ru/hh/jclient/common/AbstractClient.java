package ru.hh.jclient.common;

import static com.google.common.base.Preconditions.checkArgument;
import com.ning.http.client.RequestBuilder;
import ru.hh.jclient.common.util.jersey.UriBuilder;

public abstract class AbstractClient {

  public static final String HTTP_GET = "GET";
  public static final String HTTP_POST = "POST";
  public static final String HTTP_PUT = "PUT";
  public static final String HTTP_DELETE = "DELETE";

  protected String host;
  protected HttpClientBuilder http;

  protected AbstractClient(String host, HttpClientBuilder http) {
    this.host = host;
    this.http = http;
  }

  protected AbstractClient(String host, String path, HttpClientBuilder http) {
    this(host + path, http);
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
}