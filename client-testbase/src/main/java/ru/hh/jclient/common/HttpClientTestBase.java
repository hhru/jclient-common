package ru.hh.jclient.common;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import ru.hh.jclient.common.HttpClientImpl.CompletionHandler;
import ru.hh.jclient.common.util.storage.SingletonStorage;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;

public class HttpClientTestBase {

  static AsyncHttpClientConfig httpClientConfig = new DefaultAsyncHttpClientConfig.Builder().build();
  public static HttpClientFactory http;
  static HttpClientContext httpClientContext;
  static TestRequestDebug debug = new TestRequestDebug(true);
  static List<Supplier<RequestDebug>> debugs = List.of(() -> debug);
  static List<HttpClientEventListener> eventListeners = new ArrayList<>();

  public HttpClientTestBase withEmptyContext() {
    httpClientContext = new HttpClientContext(Collections.emptyMap(), Collections.emptyMap(), debugs);
    return this;
  }

  public HttpClientTestBase withContext(Map<String, List<String>> headers) {
    httpClientContext = new HttpClientContext(headers, Collections.emptyMap(), debugs);
    return this;
  }

  public HttpClientTestBase withContext(Map<String, List<String>> headers, Map<String, List<String>> queryParams) {
    httpClientContext = new HttpClientContext(headers, queryParams, debugs);
    return this;
  }

  public Supplier<Request> okRequest(String text, MediaType contentType) {
    return request(text, contentType, 200);
  }

  public Supplier<Request> request(String text, MediaType contentType, int status) {
    final Charset charset = contentType.charset().isPresent() ? contentType.charset().get() : Charset.defaultCharset();
    return request(text.getBytes(charset), contentType, status);
  }

  public Supplier<Request> okRequest(byte[] data, MediaType contentType) {
    return request(data, contentType, 200);
  }

  public Supplier<Request> request(byte[] data, MediaType contentType, int status) {
    org.asynchttpclient.Response response = mock(org.asynchttpclient.Response.class);
    when(response.getStatusCode()).thenReturn(status);
    if (contentType != null) {
      when(response.getHeader(eq(HttpHeaders.CONTENT_TYPE))).thenReturn(contentType.toString());
    }
    when(response.getResponseBodyAsStream()).thenReturn(new ByteArrayInputStream(data));
    when(response.getResponseBodyAsBytes()).thenReturn(data);
    when(response.getResponseBody(isA(Charset.class))).then(iom -> {
      Charset charset = iom.getArgument(0);
      return new String(data, charset);
    });
    return request(new Response(response));
  }

  public Supplier<Request> request(MediaType contentType, int status) {
    org.asynchttpclient.Response response = mock(org.asynchttpclient.Response.class);
    when(response.getStatusCode()).thenReturn(status);
    if (contentType != null) {
      when(response.getHeader(eq(HttpHeaders.CONTENT_TYPE))).thenReturn(contentType.toString());
    }
    return request(new Response(response));
  }

  public Supplier<Request> request(Response response) {
    org.asynchttpclient.Request[] request = new org.asynchttpclient.Request[1];
    AsyncHttpClient httpClient = mock(AsyncHttpClient.class);
    when(httpClient.getConfig()).thenReturn(httpClientConfig);
    when(httpClient.executeRequest(isA(org.asynchttpclient.Request.class), isA(CompletionHandler.class))).then(iom -> {
      request[0] = iom.getArgument(0);
      CompletionHandler handler = iom.getArgument(1);
      handler.onCompleted(response.getDelegate());
      return null;
    });
    http = createHttpClientBuilder(httpClient);
    return () -> new Request(request[0]);
  }

  public void assertEqualRequests(Request request1, Request request2) {
    assertEquals(request1.getUrl(), request2.getUrl());
    assertEquals(request1.getMethod(), request2.getMethod());
    ru.hh.jclient.common.HttpHeaders headers2 = request2.getHeaders();
    headers2.remove(ACCEPT);
    headers2.remove(HttpHeaderNames.X_OUTER_TIMEOUT_MS);
    assertEquals(request1.getHeaders(), headers2);
  }

  public void assertProperAcceptHeader(ResultProcessor<?> resultProcessor, Request actualRequest) {
    if (!resultProcessor.getConverter().getSupportedMediaTypes().isPresent()) {
      assertFalse(actualRequest.getHeaders().contains(ACCEPT));
      return;
    }

    Collection<MediaType> mediaTypes = resultProcessor.getConverter().getSupportedMediaTypes().get();
    assertEquals(1, actualRequest.getHeaders().getAll(ACCEPT).size());
    List<String> acceptTypes = Arrays.asList(actualRequest.getHeaders().get(ACCEPT).split(","));
    mediaTypes.forEach(type -> assertTrue(acceptTypes.contains(type.toString())));
  }

  public static <T> CompletableFuture<ResultWithStatus<T>> success(T value) {
    return completedFuture(new ResultWithStatus<>(value, 200));
  }

  public static CompletableFuture<ResultWithStatus<Void>> noContent() {
    return completedFuture(new ResultWithStatus<>(null, 204));
  }

  public static CompletableFuture<EmptyWithStatus> emptyResponse() {
    return completedFuture(new EmptyWithStatus(204));
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

  public HttpClientTestBase withNoListeners() {
    eventListeners = List.of();
    return this;
  }

  public HttpClientTestBase withEventListener(HttpClientEventListener listener) {
    eventListeners = List.of(listener);
    return this;
  }

  HttpClientFactory createHttpClientBuilder(AsyncHttpClient httpClient) {
    return new HttpClientFactory(httpClient, singleton("http://localhost"),
        new SingletonStorage<>(() -> httpClientContext),
        Runnable::run,
        new DefaultUpstreamManager(),
        eventListeners
    );
  }
}
