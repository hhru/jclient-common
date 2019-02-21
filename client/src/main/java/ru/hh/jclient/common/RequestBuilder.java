package ru.hh.jclient.common;

import static java.util.stream.Collectors.toList;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.asynchttpclient.request.body.multipart.Part;

public class RequestBuilder {

  private final org.asynchttpclient.RequestBuilder delegate;

  public RequestBuilder() {
    delegate = new org.asynchttpclient.RequestBuilder();
  }

  public RequestBuilder(String method) {
    delegate = new org.asynchttpclient.RequestBuilder(method, false, false);
  }

  public RequestBuilder(String method, boolean disableUrlEncoding) {
    delegate = new org.asynchttpclient.RequestBuilder(method, disableUrlEncoding, false);
  }

  public RequestBuilder(String method, boolean disableUrlEncoding, boolean validateHeaders) {
    delegate = new org.asynchttpclient.RequestBuilder(method, disableUrlEncoding, validateHeaders);
  }

  public RequestBuilder(Request prototype) {
    delegate = new org.asynchttpclient.RequestBuilder(prototype.getDelegate(), false, false);
  }

  public RequestBuilder(Request prototype, boolean disableUrlEncoding) {
    delegate = new org.asynchttpclient.RequestBuilder(prototype.getDelegate(), disableUrlEncoding, false);
  }

  public RequestBuilder(Request prototype, boolean disableUrlEncoding, boolean validateHeaders) {
    delegate = new org.asynchttpclient.RequestBuilder(prototype.getDelegate(), disableUrlEncoding, validateHeaders);
  }

  public RequestBuilder setCookies(Collection<Cookie> cookies) {
    delegate.setCookies(cookies.stream().map(Cookie::getDelegate).collect(toList()));
    return this;
  }

  public RequestBuilder addCookie(Cookie cookie) {
    delegate.addCookie(cookie.getDelegate());
    return this;
  }

  public RequestBuilder addOrReplaceCookie(Cookie c) {
    delegate.addOrReplaceCookie(c.getDelegate());
    return this;
  }

  public RequestBuilder resetCookies() {
    delegate.resetCookies();
    return this;
  }

  public RequestBuilder addHeader(CharSequence name, String value) {
    if (value == null) {
      value = ""; // Preserve AsynHttpClient behaviour, but without excessive warnings
    }
    delegate.addHeader(name.toString(), value);
    return this;
  }

  public RequestBuilder addFormParam(String key, String value) {
    delegate.addFormParam(key, value);
    return this;
  }

  public RequestBuilder resetFormParams() {
    delegate.resetFormParams();
    return this;
  }

  public RequestBuilder addQueryParam(String name, String value) {
    delegate.addQueryParam(name, value);
    return this;
  }

  public RequestBuilder addQueryParams(List<Param> queryParams) {
    delegate.addQueryParams(queryParams.stream().map(Param::getDelegate).collect(toList()));
    return this;
  }

  public RequestBuilder setQueryParams(List<Param> params) {
    delegate.setQueryParams(params.stream().map(Param::getDelegate).collect(toList()));
    return this;
  }

  public RequestBuilder setQueryParams(Map<String, List<String>> params) {
    delegate.setQueryParams(params);
    return this;
  }

  public RequestBuilder resetQuery() {
    delegate.resetQuery();
    return this;
  }

  public Request build() {
    return new Request(delegate.build());
  }

  public RequestBuilder setBody(byte[] data) {
    delegate.setBody(data);
    return this;
  }

  @SuppressWarnings("deprecation")
  public RequestBuilder setBody(InputStream stream) {
    delegate.setBody(stream); // it appears again in ning2
    return this;
  }

  public RequestBuilder setBody(String data) {
    delegate.setBody(data);
    return this;
  }

  public RequestBuilder setHeader(CharSequence name, String value) {
    delegate.setHeader(name.toString(), value);
    return this;
  }

  public RequestBuilder setHeaders(HttpHeaders headers) {
    delegate.setHeaders(headers.getDelegate());
    return this;
  }

  public RequestBuilder setHeaders(Map<CharSequence, ? extends Iterable<?>> headers) {
    delegate.setHeaders(headers);
    return this;
  }

  public RequestBuilder clearHeaders() {
    delegate.clearHeaders();
    return this;
  }

  public RequestBuilder setFormParams(List<Param> params) {
    delegate.setFormParams(params.stream().map(Param::getDelegate).collect(toList()));
    return this;
  }

  public RequestBuilder setFormParams(Map<String, List<String>> params) {
    delegate.setFormParams(params);
    return this;
  }

  public RequestBuilder addBodyPart(Part bodyPart) {
    delegate.addBodyPart(bodyPart);
    return this;
  }

  public RequestBuilder setMethod(String method) {
    delegate.setMethod(method);
    return this;
  }

  public RequestBuilder setUrl(String url) {
    delegate.setUrl(url);
    return this;
  }

  public RequestBuilder setUri(Uri uri) {
    delegate.setUri(uri.getDelegate());
    return this;
  }

  public RequestBuilder setAddress(InetAddress address) {
    delegate.setAddress(address);
    return this;
  }

  public RequestBuilder setLocalAddress(InetAddress address) {
    delegate.setLocalAddress(address);
    return this;
  }

  public RequestBuilder setVirtualHost(String virtualHost) {
    delegate.setVirtualHost(virtualHost);
    return this;
  }

  public RequestBuilder setFollowRedirect(boolean followRedirects) {
    delegate.setFollowRedirect(followRedirects);
    return this;
  }

  public RequestBuilder setRequestTimeout(int requestTimeout) {
    delegate.setRequestTimeout(requestTimeout);
    return this;
  }

  public RequestBuilder setRangeOffset(long rangeOffset) {
    delegate.setRangeOffset(rangeOffset);
    return this;
  }

  public RequestBuilder setCharset(Charset charset) {
    delegate.setCharset(charset);
    return this;
  }

  org.asynchttpclient.RequestBuilder getDelegate() {
    return delegate;
  }
}
