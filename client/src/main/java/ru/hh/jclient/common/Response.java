package ru.hh.jclient.common;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

public class Response {

  private final org.asynchttpclient.Response delegate;

  Response(org.asynchttpclient.Response delegate) {
    this.delegate = delegate;
  }

  protected Response() { this(null); }

  /**
   * Returns the status code for the request.
   *
   * @return The status code
   */
  public int getStatusCode() {
    return delegate.getStatusCode();
  }

  /**
   * Returns the status text for the request.
   *
   * @return The status text
   */
  public String getStatusText() {
    return delegate.getStatusText();
  }

  /**
   * Return the entire response body as a byte[].
   *
   * @return the entire response body as a byte[].
   */
  public byte[] getResponseBodyAsBytes() {
    return delegate.getResponseBodyAsBytes();
  }

  /**
   * Return the entire response body as a ByteBuffer.
   *
   * @return the entire response body as a ByteBuffer.
   */
  public ByteBuffer getResponseBodyAsByteBuffer() {
    return delegate.getResponseBodyAsByteBuffer();
  }

  /**
   * Returns an input stream for the response body. Note that you should not try to get this more than once, and that you should not close the stream.
   *
   * @return The input stream
   */
  public InputStream getResponseBodyAsStream() {
    return delegate.getResponseBodyAsStream();
  }

  /**
   * Return the entire response body as a String.
   *
   * @param charset the charset to use when decoding the stream
   * @return the entire response body as a String.
   */
  public String getResponseBody(Charset charset) {
    return delegate.getResponseBody(charset);
  }

  /**
   * Return the entire response body as a String.
   *
   * @return the entire response body as a String.
   */
  public String getResponseBody() {
    return delegate.getResponseBody();
  }

  /**
   * Return the request {@link Uri}. Note that if the request got redirected, the value of the {@link Uri} will be the last valid redirect url.
   *
   * @return the request {@link Uri}.
   */
  public Uri getUri() {
    return new Uri(delegate.getUri());
  }

  /**
   * Return the content-type header value.
   *
   * @return the content-type header value.
   */
  public String getContentType() {
    return delegate.getContentType();
  }

  /**
   * @param name the header name
   * @return the first response header value
   */
  public String getHeader(String name) {
    return delegate.getHeader(name);
  }

  /**
   * Return a {@link List} of the response header value.
   *
   * @param name the header name
   * @return the response header value
   */
  public List<String> getHeaders(String name) {
    return delegate.getHeaders(name);
  }

  public HttpHeaders getHeaders() {
    return new HttpHeaders(delegate.getHeaders());
  }

  /**
   * Return true if the response redirects to another object.
   *
   * @return True if the response redirects to another object.
   */
  public boolean isRedirected() {
    return delegate.isRedirected();
  }

  /**
   * Subclasses SHOULD implement toString() in a way that identifies the response for logging.
   *
   * @return the textual representation
   */
  @Override
  public String toString() {
    return delegate.toString();
  }

  /**
   * Return the list of {@link Cookie}.
   */
  public List<Cookie> getCookies() {
    return delegate.getCookies().stream().map(Cookie::new).collect(Collectors.toList());
  }

  /**
   * @return true if the response's status has been computed
   */
  public boolean hasResponseStatus() {
    return delegate.hasResponseStatus();
  }

  /**
   * @return true if the response's headers has been computed
   */
  public boolean hasResponseHeaders() {
    return delegate.hasResponseHeaders();
  }

  /**
   * @return true if the response's body has been computed
   */
  public boolean hasResponseBody() {
    return delegate.hasResponseBody();
  }

  org.asynchttpclient.Response getDelegate() {
    return delegate;
  }
}
