package ru.hh.jclient.common;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.Cookie;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import org.asynchttpclient.Response;
import org.asynchttpclient.uri.Uri;
import ru.hh.jclient.common.util.ContentType;

/**
 * This implementation of Response is returned in case when transport exception is mapped to status code. It is supposed to emulate intbal error
 * response.
 */
public class MappedTransportErrorResponse implements Response {

  private final int statusCode;
  private final String statusText;
  private final Uri uri;

  private static final String CONTENT_TYPE = ContentType.TEXT_PLAIN;

  private static final HttpHeaders HEADERS = new DefaultHttpHeaders();

  static {
    HEADERS.add(HttpHeaderNames.CONTENT_TYPE, CONTENT_TYPE);
  }

  public MappedTransportErrorResponse(int statusCode, String statusText, ru.hh.jclient.common.Uri uri) {
    this(statusCode, statusText, uri.getDelegate());
  }

  public MappedTransportErrorResponse(int statusCode, String statusText, Uri uri) {
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
  public byte[] getResponseBodyAsBytes() {
    return statusText.getBytes();
  }

  @Override
  public ByteBuffer getResponseBodyAsByteBuffer() {
    return ByteBuffer.wrap(getResponseBodyAsBytes());
  }

  @Override
  public InputStream getResponseBodyAsStream() {
    return new ByteArrayInputStream(getResponseBodyAsBytes());
  }

  @Override
  public String getResponseBody(Charset charset) {
    if (charset == null) {
      return statusText;
    }
    return new String(statusText.getBytes(), charset);
  }

  @Override
  public String getResponseBody() {
    return statusText;
  }

  @Override
  public Uri getUri() {
    return uri;
  }

  @Override
  public String getContentType() {
    return CONTENT_TYPE;
  }

  @Override
  public String getHeader(CharSequence name) {
    return HEADERS.get(name);
  }

  @Override
  public List<String> getHeaders(CharSequence name) {
    return HEADERS.getAll(name);
  }

  @Override
  public HttpHeaders getHeaders() {
    return HEADERS;
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
    return true;
  }

  @Override
  public boolean hasResponseBody() {
    return true;
  }

  @Override
  public SocketAddress getRemoteAddress() {
    return null;
  }

  @Override
  public SocketAddress getLocalAddress() {
    return null;
  }
}
