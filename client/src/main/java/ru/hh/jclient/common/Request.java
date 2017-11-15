package ru.hh.jclient.common;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Request {

  private final com.ning.http.client.Request delegate;

  Request(com.ning.http.client.Request request) {
    this.delegate = request;
  }

  /**
   * Return the request's method name (GET, POST, etc.)
   *
   * @return the request's method name (GET, POST, etc.)
   */
  public String getMethod() {
    return delegate.getMethod();
  }

  public Uri getUri() {
    return new Uri(delegate.getUri());
  }

  public String getUrl() {
    return delegate.getUrl();
  }

  /**
   * Return the InetAddress to override
   *
   * @return the InetAddress
   */
  public InetAddress getInetAddress() {
    return delegate.getInetAddress();
  }

  public InetAddress getLocalAddress() {
    return delegate.getLocalAddress();
  }

  /**
   * Return the current set of Headers.
   *
   * @return a {@link Map} contains headers.
   */
  public Map<String, List<String>> getHeaders() {
    return delegate.getHeaders();
  }

  /**
   * Return Coookie.
   *
   * @return an unmodifiable Collection of Cookies
   */
  public Collection<Cookie> getCookies() {
    return unmodifiableCollection(delegate.getCookies().stream().map(Cookie::new).collect(toList()));
  }

  /**
   * Return the current request's body as a byte array
   *
   * @return a byte array of the current request's body.
   */
  public byte[] getByteData() {
    return delegate.getByteData();
  }

  /**
   * @return the current request's body as a composite of byte arrays
   */
  public List<byte[]> getCompositeByteData() {
    return delegate.getCompositeByteData();
  }

  /**
   * Return the current request's body as a string
   *
   * @return an String representation of the current request's body.
   */
  public String getStringData() {
    return delegate.getStringData();
  }

  /**
   * Return the current request's body as an InputStream
   *
   * @return an InputStream representation of the current request's body.
   */
  public InputStream getStreamData() {
    return delegate.getStreamData();
  }

  /**
   * Return the current size of the content-lenght header based on the body's size.
   *
   * @return the current size of the content-lenght header based on the body's size.
   */
  public long getContentLength() {
    return delegate.getContentLength();
  }

  /**
   * Return the current form parameters as unmodifiable list.
   *
   * @return a {@link List<Param>} of parameters.
   */
  public List<Param> getFormParams() {
    return unmodifiableList(delegate.getFormParams().stream().map(Param::new).collect(toList()));
  }

  /**
   * Return the virtual host value.
   *
   * @return the virtual host value.
   */
  public String getVirtualHost() {
    return delegate.getVirtualHost();
  }

  /**
   * Return the query params as unmodifiable list.
   *
   * @return {@link List<Param>} of query string
   */
  public List<Param> getQueryParams() {
    return unmodifiableList(delegate.getQueryParams().stream().map(Param::new).collect(toList()));
  }

  /**
   * Return the {@link File} to upload.
   *
   * @return the {@link File} to upload.
   */
  public File getFile() {
    return delegate.getFile();
  }

  /**
   * Return follow redirect
   *
   * @return the <tt>TRUE></tt> to follow redirect, FALSE, if NOT to follow, whatever the client config. Return null if not set.
   */
  public Boolean getFollowRedirect() {
    return delegate.getFollowRedirect();
  }

  /**
   * Overrides the config default value
   *
   * @return the request timeout
   */
  public int getRequestTimeout() {
    return delegate.getRequestTimeout();
  }

  /**
   * Return the HTTP Range header value, or
   *
   * @return the range header value, or 0 is not set.
   */
  public long getRangeOffset() {
    return delegate.getRangeOffset();
  }

  /**
   * Return the encoding value used when encoding the request's body.
   *
   * @return the encoding value used when encoding the request's body.
   */
  public String getBodyEncoding() {
    return delegate.getBodyEncoding();
  }

  com.ning.http.client.Request getDelegate() {
    return delegate;
  }
}
