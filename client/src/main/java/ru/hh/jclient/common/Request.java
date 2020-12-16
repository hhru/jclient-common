package ru.hh.jclient.common;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

public class Request {

  private final org.asynchttpclient.Request delegate;

  Request(org.asynchttpclient.Request request) {
    this.delegate = request;
  }

  /**
   * @return the request's HTTP method (GET, POST, etc.)
   */
  public String getMethod() {
    return delegate.getMethod();
  }

  /**
   * @return the uri
   */
  public Uri getUri() {
    return new Uri(delegate.getUri());
  }

  /**
   * @return the url (the uri's String form)
   */
  public String getUrl() {
    return delegate.getUrl();
  }

  /**
   * @return the InetAddress to be used to bypass uri's hostname resolution
   */
  public InetAddress getAddress() {
    return delegate.getAddress();
  }

  /**
   * @return the local address to bind from
   */
  public InetAddress getLocalAddress() {
    return delegate.getLocalAddress();
  }

  /**
   * @return the HTTP headers
   */
  public HttpHeaders getHeaders() {
    return new HttpHeaders(delegate.getHeaders());
  }

  /**
   * @return the HTTP cookies
   */
  public Collection<Cookie> getCookies() {
    return unmodifiableCollection(delegate.getCookies().stream().map(Cookie::new).collect(toList()));
  }

  /**
   * @return the request's body byte array (only non null if it was set this way)
   */
  public byte[] getByteData() {
    return delegate.getByteData();
  }

  /**
   * @return the request's body array of byte arrays (only non null if it was set this way)
   */
  public List<byte[]> getCompositeByteData() {
    return delegate.getCompositeByteData();
  }

  /**
   * @return the request's body string (only non null if it was set this way)
   */
  public String getStringData() {
    return delegate.getStringData();
  }

  /**
   * @return the request's body InputStream (only non null if it was set this way)
   */
  public InputStream getStreamData() {
    return delegate.getStreamData();
  }

  /**
   * @return the request's form parameters
   */
  public List<Param> getFormParams() {
    return unmodifiableList(delegate.getFormParams().stream().map(Param::new).collect(toList()));
  }

  /**
   * @return the virtual host to connect to
   */
  public String getVirtualHost() {
    return delegate.getVirtualHost();
  }

  /**
   * @return the query params resolved from the url/uri
   */
  public List<Param> getQueryParams() {
    return unmodifiableList(delegate.getQueryParams().stream().map(Param::new).collect(toList()));
  }

  /**
   * @return the file to be uploaded
   */
  public File getFile() {
    return delegate.getFile();
  }

  /**
   * @return if this request is to follow redirects. Non null values means "override config value".
   */
  public Boolean getFollowRedirect() {
    return delegate.getFollowRedirect();
  }

  /**
   * @return the request timeout. Non zero values means "override config value".
   */
  public int getRequestTimeout() {
    return delegate.getRequestTimeout();
  }

  /**
   * @return the read timeout. Non zero values means "override config value".
   */
  public int getReadTimeout() {
    return delegate.getReadTimeout();
  }

  /**
   * @return the range header value, or 0 is not set.
   */
  public long getRangeOffset() {
    return delegate.getRangeOffset();
  }

  /**
   * @return the charset value used when decoding the request's body.
   */
  public Charset getCharset() {
    return delegate.getCharset();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  public String toStringExtended() {
    StringBuilder sb = new StringBuilder(delegate.toString());
    sb.append("; requestTimeout:");
    sb.append(getRequestTimeout());
    sb.append("; readTimeout:");
    sb.append(getReadTimeout());
    sb.append("; queryParams:");
    sb.append(getQueryParams());
    return sb.toString();
  }

  org.asynchttpclient.Request getDelegate() {
    return delegate;
  }
}
