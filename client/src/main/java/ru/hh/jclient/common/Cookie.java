package ru.hh.jclient.common;

public class Cookie {

  private final com.ning.http.client.cookie.Cookie delegate;

  Cookie(com.ning.http.client.cookie.Cookie cookie) {
    this.delegate = cookie;
  }

  public static Cookie newValidCookie(
      String name,
      String value,
      boolean wrap,
      String domain,
      String path,
      long maxAge,
      boolean secure,
      boolean httpOnly) {
    return new Cookie(com.ning.http.client.cookie.Cookie.newValidCookie(name, value, wrap, domain, path, maxAge, secure, httpOnly));
  }

  public Cookie(String name, String value, boolean wrap, String domain, String path, long maxAge, boolean secure, boolean httpOnly) {
    this.delegate = new com.ning.http.client.cookie.Cookie(name, value, wrap, domain, path, maxAge, secure, httpOnly);
  }

  public String getDomain() {
    return delegate.getDomain();
  }

  public String getName() {
    return delegate.getName();
  }

  public String getValue() {
    return delegate.getValue();
  }

  public boolean isWrap() {
    return delegate.isWrap();
  }

  public String getPath() {
    return delegate.getPath();
  }

  public long getMaxAge() {
    return delegate.getMaxAge();
  }

  public boolean isSecure() {
    return delegate.isSecure();
  }

  public boolean isHttpOnly() {
    return delegate.isHttpOnly();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  com.ning.http.client.cookie.Cookie getDelegate() {
    return delegate;
  }
}
