package ru.hh.jclient.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Response;
import com.ning.http.client.cookie.Cookie;
import com.ning.http.client.uri.Uri;

class ErroneousResponse implements Response {

  private final int statusCode;
  private final String statusText;
  private final Uri uri;

  private static final String EMPTY = "";

  ErroneousResponse(int statusCode, String statusText, Uri uri) {
    this.statusCode = statusCode;
    this.statusText = statusText;
    this.uri = uri;
  }

  @Override
  public int getStatusCode() {
    return statusCode;
  }

  @Override
  public String getStatusText() {
    return statusText;
  }

  @Override
  public byte[] getResponseBodyAsBytes() throws IOException {
    return new byte[0];
  }

  @Override
  public ByteBuffer getResponseBodyAsByteBuffer() throws IOException {
    return ByteBuffer.allocate(0);
  }

  @Override
  public InputStream getResponseBodyAsStream() throws IOException {
    return new ByteArrayInputStream(getResponseBodyAsBytes());
  }

  @SuppressWarnings("unused")
  @Override
  public String getResponseBodyExcerpt(int maxLength, String charset) throws IOException {
    return EMPTY;
  }

  @SuppressWarnings("unused")
  @Override
  public String getResponseBody(String charset) throws IOException {
    return EMPTY;
  }

  @SuppressWarnings("unused")
  @Override
  public String getResponseBodyExcerpt(int maxLength) throws IOException {
    return EMPTY;
  }

  @Override
  public String getResponseBody() throws IOException {
    return EMPTY;
  }

  @Override
  public Uri getUri() {
    return uri;
  }

  @Override
  public String getContentType() {
    return null;
  }

  @SuppressWarnings("unused")
  @Override
  public String getHeader(String name) {
    return null;
  }

  @SuppressWarnings("unused")
  @Override
  public List<String> getHeaders(String name) {
    return Collections.emptyList();
  }

  @Override
  public FluentCaseInsensitiveStringsMap getHeaders() {
    return new FluentCaseInsensitiveStringsMap();
  }

  @Override
  public boolean isRedirected() {
    return false;
  }

  @Override
  public List<Cookie> getCookies() {
    return Collections.emptyList();
  }

  @Override
  public boolean hasResponseStatus() {
    return true;
  }

  @Override
  public boolean hasResponseHeaders() {
    return false;
  }

  @Override
  public boolean hasResponseBody() {
    return false;
  }
}
