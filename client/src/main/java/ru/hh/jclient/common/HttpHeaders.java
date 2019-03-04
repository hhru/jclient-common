package ru.hh.jclient.common;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class HttpHeaders implements Iterable<Map.Entry<String, String>> {
  private io.netty.handler.codec.http.HttpHeaders delegate;

  public HttpHeaders() {
    this.delegate = new io.netty.handler.codec.http.DefaultHttpHeaders();
  }

  HttpHeaders(io.netty.handler.codec.http.HttpHeaders delegate) {
    this.delegate = delegate;
  }

  public HttpHeaders(Map<String, List<String>> headers) {
    this.delegate = new io.netty.handler.codec.http.DefaultHttpHeaders();
    headers.forEach((k, v) -> this.delegate.set(k, v));
  }

  public String get(String name) {
    return delegate.get(name);
  }

  public Integer getInt(CharSequence name) {
    return delegate.getInt(name);
  }

  public int getInt(CharSequence name, int defaultValue) {
    return delegate.getInt(name, defaultValue);
  }

  public Long getTimeMillis(CharSequence name) {
    return delegate.getTimeMillis(name);
  }

  public long getTimeMillis(CharSequence name, long defaultValue) {
    return delegate.getTimeMillis(name, defaultValue);
  }

  public List<String> getAll(String name) {
    return delegate.getAll(name);
  }

  public List<Map.Entry<String, String>> entries() {
    return delegate.entries();
  }

  public boolean contains(String name) {
    return delegate.contains(name);
  }

  public Stream<Map.Entry<String, String>> stream() {
    return StreamSupport.stream(delegate.spliterator(), false);
  }

  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  public int size() {
    return delegate.size();
  }

  public Set<String> names() {
    return delegate.names();
  }

  public HttpHeaders add(HttpHeaders headers) {
    delegate.add(headers.getDelegate());
    return this;
  }

  public HttpHeaders add(String name, Object value) {
    delegate.add(name, value);
    return this;
  }

  public HttpHeaders add(String name, Iterable<?> values) {
    delegate.add(name, values);
    return this;
  }

  public HttpHeaders addInt(CharSequence name, int value) {
    delegate.addInt(name, value);
    return this;
  }

  public HttpHeaders set(String name, Object value) {
    delegate.set(name, value);
    return this;
  }

  public HttpHeaders set(String name, Iterable<?> values) {
    delegate.set(name, values);
    return this;
  }

  public HttpHeaders setInt(CharSequence name, int value) {
    delegate.setInt(name, value);
    return this;
  }

  public HttpHeaders remove(String name) {
    delegate.remove(name);
    return this;
  }

  public HttpHeaders clear() {
    delegate.clear();
    return this;
  }

  @Override
  public Iterator<Map.Entry<String, String>> iterator() {
    return delegate.iterator();
  }

  @Override
  public Spliterator<Map.Entry<String, String>> spliterator() {
    return delegate.spliterator();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HttpHeaders entries = (HttpHeaders) o;
    return Objects.equals(delegate, entries.delegate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }

  io.netty.handler.codec.http.HttpHeaders getDelegate() {
    return delegate;
  }
}
