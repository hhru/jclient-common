package ru.hh.jclient.common;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import ru.hh.jclient.common.HttpClientImpl.CompletionHandler;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

public class HttpClientTestBase {

  public static AsyncHttpClientConfig httpClientConfig = new AsyncHttpClientConfig.Builder().build();
  public static HttpClientBuilder http;
  public static HttpClientContext httpClientContext;
  public static TestRequestDebug debug = new TestRequestDebug(true);

  public HttpClientTestBase withEmptyContext() {
    httpClientContext = new HttpClientContext(Collections.emptyMap(), Collections.emptyMap(), () -> debug);
    return this;
  }

  public HttpClientTestBase withContext(Map<String, List<String>> headers) {
    httpClientContext = new HttpClientContext(headers, Collections.emptyMap(), () -> debug);
    return this;
  }

  public HttpClientTestBase withContext(Map<String, List<String>> headers, Map<String, List<String>> queryParams) {
    httpClientContext = new HttpClientContext(headers, queryParams, () -> debug);
    return this;
  }

  public Supplier<Request> okRequest(String text, MediaType contentType) throws IOException {
    return request(text, contentType, 200);
  }

  public Supplier<Request> request(String text, MediaType contentType, int status) throws IOException {
    final Charset charset = contentType.charset().isPresent() ? contentType.charset().get() : Charset.defaultCharset();
    return request(text.getBytes(charset), contentType, status);
  }

  public Supplier<Request> okRequest(byte[] data, MediaType contentType) throws IOException {
    return request(data, contentType, 200);
  }

  public Supplier<Request> request(byte[] data, MediaType contentType, int status) throws IOException {
    Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(status);
    if (contentType != null) {
      when(response.getHeader(eq(HttpHeaders.CONTENT_TYPE))).thenReturn(contentType.toString());
    }
    when(response.getResponseBodyAsStream()).thenReturn(new ByteArrayInputStream(data));
    when(response.getResponseBodyAsBytes()).thenReturn(data);
    when(response.getResponseBody(isA(String.class))).then(iom -> {
      String charsetName = iom.getArgumentAt(0, String.class);
      return new String(data, Charset.forName(charsetName));
    });
    return request(response);
  }

  public Supplier<Request> request(MediaType contentType, int status) {
    Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(status);
    if (contentType != null) {
      when(response.getHeader(eq(HttpHeaders.CONTENT_TYPE))).thenReturn(contentType.toString());
    }
    return request(response);
  }

  public Supplier<Request> request(Response response) {
    Request[] request = new Request[1];
    AsyncHttpClient httpClient = mock(AsyncHttpClient.class);
    when(httpClient.getConfig()).thenReturn(httpClientConfig);
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class))).then(iom -> {
      request[0] = iom.getArgumentAt(0, Request.class);
      CompletionHandler handler = iom.getArgumentAt(1, CompletionHandler.class);
      handler.onCompleted(response);
      return null;
    });
    http = new HttpClientBuilder(httpClient, ImmutableSet.of("http://localhost"), () -> httpClientContext);
    return () -> request[0];
  }

  public void assertEqualRequests(Request request1, Request request2) {
    assertEquals(request1.getUrl(), request2.getUrl());
    assertEquals(request1.getMethod(), request2.getMethod());
    FluentCaseInsensitiveStringsMap headers2 = request2.getHeaders();
    headers2.remove(HttpHeaders.ACCEPT);
    assertEquals(request1.getHeaders(), headers2);
  }

  public static <T> CompletableFuture<ResultWithStatus<T>> success(T value) {
    return completedFuture(new ResultWithStatus<>(value, 200));
  }

  public static CompletableFuture<ResultWithStatus<Void>> noContent() {
    return completedFuture(new ResultWithStatus<>(null, 204));
  }

  public static <T> CompletableFuture<ResultWithStatus<T>> error(int status) {
    return completedFuture(new ResultWithStatus<>(null, status));
  }

  public static <T, E> CompletableFuture<ResultOrErrorWithStatus<T, E>> orErrorSuccess(T value) {
    return completedFuture(new ResultOrErrorWithStatus<>(ofNullable(value), empty(), 200));
  }

  public static <T, E> CompletableFuture<ResultOrErrorWithStatus<T, E>> orErrorNoContent() {
    return completedFuture(new ResultOrErrorWithStatus<>(empty(), empty(), 204));
  }

  public static <T, E> CompletableFuture<ResultOrErrorWithStatus<T, E>> orError(int status, E error) {
    return completedFuture(new ResultOrErrorWithStatus<>(empty(), of(error), status));
  }

}
