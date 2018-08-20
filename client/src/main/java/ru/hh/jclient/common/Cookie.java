package ru.hh.jclient.common;

public class Cookie {

  private final io.netty.handler.codec.http.cookie.Cookie delegate;

  Cookie(io.netty.handler.codec.http.cookie.Cookie cookie) {
    this.delegate = cookie;
  }

  public Cookie(String name, String value, boolean wrap, String domain, String path, long maxAge, boolean secure, boolean httpOnly) {
    this.delegate = new io.netty.handler.codec.http.cookie.DefaultCookie(name, value);
    this.delegate.setWrap(wrap);
    this.delegate.setDomain(domain);
    this.delegate.setPath(path);
    this.delegate.setMaxAge(maxAge);
    this.delegate.setSecure(secure);
    this.delegate.setHttpOnly(httpOnly);
  }

  public String getDomain() {
    return delegate.domain();
  }

  public String getName() {
    return delegate.name();
  }

  public String getValue() {
    return delegate.value();
  }

  public boolean isWrap() {
    return delegate.wrap();
  }

  public String getPath() {
    return delegate.path();
  }

  public long getMaxAge() {
    return delegate.maxAge();
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

  io.netty.handler.codec.http.cookie.Cookie getDelegate() {
    return delegate;
  }
}
