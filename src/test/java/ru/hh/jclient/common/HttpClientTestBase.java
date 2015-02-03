package ru.hh.jclient.common;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import ru.hh.jclient.common.HttpClientImpl.CompletionHandler;

import com.google.common.collect.ImmutableSet;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

public class HttpClientTestBase {

  protected static AsyncHttpClientConfig httpClientConfig = new AsyncHttpClientConfig.Builder().build();
  protected static HttpClientBuilder http;
  protected static HttpClientContext httpClientContext;
  protected static TestRequestDebug debug = new TestRequestDebug();

  protected HttpClientTestBase withEmptyContext() {
    httpClientContext = new HttpClientContext(Collections.<String, List<String>> emptyMap(), () -> debug);
    return this;
  }

  protected HttpClientTestBase withContext(Map<String, List<String>> headers) {
    httpClientContext = new HttpClientContext(headers, () -> new TestRequestDebug());
    return this;
  }

  protected Supplier<Request> okRequest(String text) throws IOException {
    return request(text, 200);
  }

  protected Supplier<Request> request(String text, int status) throws IOException {
    Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(status);
    when(response.getResponseBody(isA(String.class))).thenReturn(text);
    return request(response);
  }

  protected Supplier<Request> okRequest(byte[] data) throws IOException {
    return request(data, 200);
  }

  protected Supplier<Request> request(byte[] data, int status) throws IOException {
    Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(status);
    when(response.getResponseBodyAsStream()).thenReturn(new ByteArrayInputStream(data));
    when(response.getResponseBodyAsBytes()).thenReturn(data);
    when(response.getResponseBody(isA(String.class))).then(iom -> {
      String charsetName = iom.getArgumentAt(0, String.class);
      return new String(data, Charset.forName(charsetName));
    });
    return request(response);
  }

  protected Supplier<Request> request(int status) {
    Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(status);
    return request(response);
  }

  protected Supplier<Request> request(Response response) {
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

  protected void assertEqualRequests(Request request1, Request request2) {
    assertEquals(request1.getUrl(), request2.getUrl());
    assertEquals(request1.getMethod(), request2.getMethod());
    assertEquals(request1.getHeaders(), request2.getHeaders());
  }

}
