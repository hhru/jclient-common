package ru.hh.jclient.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import com.google.common.net.MediaType;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Response;
import com.ning.http.client.cookie.Cookie;
import com.ning.http.client.uri.Uri;

/**
 * This implementation of Response is returned in case when transport exception is mapped to status code. It is supposed to emulate intbal error
 * response.
 */
public class MappedTransportErrorResponse implements Response {

  private final int statusCode;
  private final String statusText;
  private final Uri uri;

  private static final String CONTENT_TYPE = MediaType.PLAIN_TEXT_UTF_8.withoutParameters().toString();

  private static final FluentCaseInsensitiveStringsMap HEADERS = new FluentCaseInsensitiveStringsMap();

  static {
    HEADERS.add("Content-Type", CONTENT_TYPE);
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
  public byte[] getResponseBodyAsBytes() throws IOException {
    return statusText.getBytes();
  }

  @Override
  public ByteBuffer getResponseBodyAsByteBuffer() throws IOException {
    return ByteBuffer.wrap(getResponseBodyAsBytes());
  }

  @Override
  public InputStream getResponseBodyAsStream() throws IOException {
    return new ByteArrayInputStream(getResponseBodyAsBytes());
  }

  @Override
  public String getResponseBodyExcerpt(int maxLength, String charset) throws IOException {
    String response = getResponseBody(charset);
    return response.length() <= maxLength ? response : response.substring(0, maxLength);
  }

  @Override
  public String getResponseBody(String charset) throws IOException {
    if (charset == null) {
      return statusText;
    }
    return new String(statusText.getBytes(), Charset.forName(charset));
  }

  @Override
  public String getResponseBodyExcerpt(int maxLength) throws IOException {
    return getResponseBodyExcerpt(maxLength, null);
  }

  @Override
  public String getResponseBody() throws IOException {
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
  public String getHeader(String name) {
    return HEADERS.getFirstValue(name);
  }

  @Override
  public List<String> getHeaders(String name) {
    return HEADERS.get(name);
  }

  @Override
  public FluentCaseInsensitiveStringsMap getHeaders() {
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
}
