package ru.hh.jclient.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Response {

  private final com.ning.http.client.Response delegate;

  Response(com.ning.http.client.Response delegate) {
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
   * @throws IOException
   */
  public byte[] getResponseBodyAsBytes() throws IOException {
    return delegate.getResponseBodyAsBytes();
  }

  /**
   * Return the entire response body as a ByteBuffer.
   *
   * @return the entire response body as a ByteBuffer.
   * @throws IOException
   */
  public ByteBuffer getResponseBodyAsByteBuffer() throws IOException {
    return delegate.getResponseBodyAsByteBuffer();
  }

  /**
   * Returns an input stream for the response body. Note that you should not try to get this more than once, and that you should not close the stream.
   *
   * @return The input stream
   * @throws java.io.IOException
   */
  public InputStream getResponseBodyAsStream() throws IOException {
    return delegate.getResponseBodyAsStream();
  }

  /**
   * Returns the first maxLength bytes of the response body as a string. Note that this does not check whether the content type is actually a textual
   * one, but it will use the charset if present in the content type header.
   *
   * @param maxLength The maximum number of bytes to read
   * @param charset   the charset to use when decoding the stream
   * @return The response body
   * @throws java.io.IOException
   */
  public String getResponseBodyExcerpt(int maxLength, String charset) throws IOException {
    return delegate.getResponseBodyExcerpt(maxLength, charset);
  }

  /**
   * Return the entire response body as a String.
   *
   * @param charset the charset to use when decoding the stream
   * @return the entire response body as a String.
   * @throws IOException
   */
  public String getResponseBody(String charset) throws IOException {
    return delegate.getResponseBody(charset);
  }

  /**
   * Returns the first maxLength bytes of the response body as a string. Note that this does not check whether the content type is actually a textual
   * one, but it will use the charset if present in the content type header.
   *
   * @param maxLength The maximum number of bytes to read
   * @return The response body
   * @throws java.io.IOException
   */
  public String getResponseBodyExcerpt(int maxLength) throws IOException {
    return delegate.getResponseBodyExcerpt(maxLength);
  }

  /**
   * Return the entire response body as a String.
   *
   * @return the entire response body as a String.
   * @throws IOException
   */
  public String getResponseBody() throws IOException {
    return delegate.getResponseBody();
  }

  /**
   * Return the request {@link Uri}. Note that if the request got redirected, the value of the {@link URI} will be the last valid redirect url.
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
   * Return the response header
   *
   * @return the response header
   */
  public String getHeader(String name) {
    return delegate.getHeader(name);
  }

  /**
   * Return a {@link List} of the response header value.
   *
   * @return the response header
   */
  public List<String> getHeaders(String name) {
    return delegate.getHeaders(name);
  }

  public Map<String, List<String>> getHeaders() {
    return delegate.getHeaders();
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
   * @return The textual representation
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
   * Return true if the response's status has been computed by an {@link AsyncHandler}
   *
   * @return true if the response's status has been computed by an {@link AsyncHandler}
   */
  public boolean hasResponseStatus() {
    return delegate.hasResponseStatus();
  }

  /**
   * Return true if the response's headers has been computed by an {@link AsyncHandler} It will return false if the either
   * {@link AsyncHandler#onStatusReceived(HttpResponseStatus)} or {@link AsyncHandler#onHeadersReceived(HttpResponseHeaders)} returned
   * {@link AsyncHandler.STATE#ABORT}
   *
   * @return true if the response's headers has been computed by an {@link AsyncHandler}
   */
  public boolean hasResponseHeaders() {
    return delegate.hasResponseHeaders();
  }

  /**
   * Return true if the response's body has been computed by an {@link AsyncHandler}. It will return false if the either
   * {@link AsyncHandler#onStatusReceived(HttpResponseStatus)} or {@link AsyncHandler#onHeadersReceived(HttpResponseHeaders)} returned
   * {@link AsyncHandler.STATE#ABORT}
   *
   * @return true if the response's body has been computed by an {@link AsyncHandler}
   */
  public boolean hasResponseBody() {
    return delegate.hasResponseBody();
  }

  com.ning.http.client.Response getDelegate() {
    return delegate;
  }
}
