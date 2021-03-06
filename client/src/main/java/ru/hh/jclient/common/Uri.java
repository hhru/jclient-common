package ru.hh.jclient.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class Uri {

  private final org.asynchttpclient.uri.Uri delegate;

  Uri(org.asynchttpclient.uri.Uri delegate) {
    this.delegate = delegate;
  }

  public static Uri create(String originalUrl) {
    return new Uri(org.asynchttpclient.uri.Uri.create(null, originalUrl));
  }

  public static Uri create(Uri context, final String originalUrl) {
    return new Uri(org.asynchttpclient.uri.Uri.create(context.getDelegate(), originalUrl));
  }

  public Uri(String scheme, String userInfo, String host, int port, String path, String query) {
    this.delegate = new org.asynchttpclient.uri.Uri(scheme, userInfo, host, port, path, query, null);
  }

  public String getQuery() {
    return delegate.getQuery();
  }

  public String getPath() {
    return delegate.getPath();
  }

  public String getUserInfo() {
    return delegate.getUserInfo();
  }

  public int getPort() {
    return delegate.getPort();
  }

  public String getScheme() {
    return delegate.getScheme();
  }

  public String getHost() {
    return delegate.getHost();
  }

  public URI toJavaNetURI() throws URISyntaxException {
    return delegate.toJavaNetURI();
  }

  public String toUrl() {
    return delegate.toUrl();
  }

  public String toRelativeUrl() {
    return delegate.toRelativeUrl();
  }

  @Override
  public String toString() {
    if (delegate == null) { // can happen when mock response in tests
      return null;
    }
    return delegate.toString();
  }

  public Uri withNewScheme(String newScheme) {
    return new Uri(delegate.withNewScheme(newScheme));
  }

  public Uri withNewQuery(String newQuery) {
    return new Uri(delegate.withNewQuery(newQuery));
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return Objects.equals(obj, this.delegate);
  }

  org.asynchttpclient.uri.Uri getDelegate() {
    return delegate;
  }
}
