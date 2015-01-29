package ru.hh.jclient.common;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static ru.hh.jclient.common.HttpHeaders.X_HH_DEBUG;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.junit.Test;

import ru.hh.jclient.common.HttpClientImpl.CompletionHandler;
import ru.hh.jclient.common.exception.ClientResponseException;
import ru.hh.jclient.common.exception.ResponseConverterException;
import ru.hh.jclient.common.model.ProtobufTest;
import ru.hh.jclient.common.model.ProtobufTest.ProtobufTestMessage;
import ru.hh.jclient.common.model.XmlTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;

public class HttpClientTest {

  private static AsyncHttpClientConfig httpClientConfig = new AsyncHttpClientConfig.Builder().build();
  private static HttpClientBuilder http;
  private static HttpClientContext httpClientContext;

  private HttpClientTest withEmptyContext() {
    httpClientContext = new HttpClientContext(Collections.<String, List<String>> emptyMap(), () -> new TestRequestDebug());
    return this;
  }

  private HttpClientTest withContext(Map<String, List<String>> headers) {
    httpClientContext = new HttpClientContext(headers, () -> new TestRequestDebug());
    return this;
  }

  private Supplier<Request> mockRequest(String text) throws IOException {
    Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(200);
    when(response.getResponseBody(any())).thenReturn(text);
    return mockRequest(response);
  }

  private Supplier<Request> mockRequest(byte[] data) throws IOException {
    Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(200);
    when(response.getResponseBodyAsStream()).thenReturn(new ByteArrayInputStream(data));
    when(response.getResponseBodyAsBytes()).thenReturn(data);
    return mockRequest(response);
  }

  private Supplier<Request> mockRequest(int status) {
    Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(status);
    return mockRequest(response);
  }

  private Supplier<Request> mockRequest(Response response) {
    Request[] request = new Request[1];
    AsyncHttpClient httpClient = mock(AsyncHttpClient.class);
    when(httpClient.getConfig()).thenReturn(httpClientConfig);
    when(httpClient.executeRequest(any(), any())).then(iom -> {
      request[0] = iom.getArgumentAt(0, Request.class);
      CompletionHandler handler = iom.getArgumentAt(1, CompletionHandler.class);
      handler.onCompleted(response);
      return null;
    });
    http = new HttpClientBuilder(httpClient, ImmutableSet.of("http://localhost"), () -> httpClientContext);
    return () -> request[0];
  }

  private void assertEqualRequests(Request request1, Request request2) {
    assertEquals(request1.getUrl(), request2.getUrl());
    assertEquals(request1.getMethod(), request2.getMethod());
    assertEquals(request1.getHeaders(), request2.getHeaders());
  }

  @Test
  public void testPlain() throws InterruptedException, ExecutionException, IOException {
    Supplier<Request> actualRequest = withEmptyContext().mockRequest("test тест");

    Request request = new RequestBuilder("GET").setUrl("http://localhost/plain").build();
    String text = http.with(request).<String> returnText().get();
    assertEquals("test тест", text);
    assertEqualRequests(request, actualRequest.get());
  }

  @Test
  public void testXml() throws InterruptedException, ExecutionException, IOException, JAXBException {
    XmlTest test = new XmlTest("test тест");
    JAXBContext context = JAXBContext.newInstance(XmlTest.class);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    context.createMarshaller().marshal(test, out);
    Supplier<Request> actualRequest = withEmptyContext().mockRequest(out.toByteArray());

    Request request = new RequestBuilder("GET").setUrl("http://localhost/xml").build();
    XmlTest testOutput = http.with(request).<XmlTest> returnXml(context).get();
    assertEquals(test.name, testOutput.name);
    assertEqualRequests(request, actualRequest.get());
  }

  @Test(expected = ResponseConverterException.class)
  public void testIncorrectXml() throws Throwable {
    withEmptyContext().mockRequest("test тест".getBytes());

    Request request = new RequestBuilder("GET").setUrl("http://localhost/xml").build();
    try {
      http.with(request).<XmlTest> returnXml(JAXBContext.newInstance(XmlTest.class)).get();
    }
    catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  @Test
  public void testJson() throws IOException, InterruptedException, ExecutionException {
    XmlTest test = new XmlTest("test тест");
    ObjectMapper objectMapper = new ObjectMapper();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    objectMapper.writeValue(out, test);
    Supplier<Request> actualRequest = withEmptyContext().mockRequest(out.toByteArray());

    Request request = new RequestBuilder("GET").setUrl("http://localhost/json").build();
    XmlTest testOutput = http.with(request).<XmlTest> returnJson(objectMapper, XmlTest.class).get();
    assertEquals(test.name, testOutput.name);
    assertEqualRequests(request, actualRequest.get());
  }

  @Test(expected = ResponseConverterException.class)
  public void testIncorrectJson() throws Throwable {
    withEmptyContext().mockRequest("test тест".getBytes());

    Request request = new RequestBuilder("GET").setUrl("http://localhost/json").build();
    try {
      http.with(request).<XmlTest> returnJson(new ObjectMapper(), XmlTest.class).get();
    }
    catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  @Test
  public void testProtobuf() throws IOException, InterruptedException, ExecutionException {
    ProtobufTest.ProtobufTestMessage test = ProtobufTest.ProtobufTestMessage.newBuilder().addIds(1).build();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    test.writeTo(out);

    Supplier<Request> actualRequest = withEmptyContext().mockRequest(out.toByteArray());

    Request request = new RequestBuilder("GET").setUrl("http://localhost/protobuf").build();
    ProtobufTestMessage testOutput = http.with(request).<ProtobufTestMessage> returnProtobuf(ProtobufTestMessage.class).get();
    assertEquals(test.getIdsList(), testOutput.getIdsList());
    assertEqualRequests(request, actualRequest.get());
  }

  @Test(expected = ResponseConverterException.class)
  public void testIncorrectProtobuf() throws Throwable {
    withEmptyContext().mockRequest("test тест".getBytes());

    Request request = new RequestBuilder("GET").setUrl("http://localhost/protobuf").build();
    try {
      http.with(request).<ProtobufTestMessage> returnProtobuf(ProtobufTestMessage.class).get();
    }
    catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  @Test
  public void testEmpty() throws IOException, InterruptedException, ExecutionException {
    Supplier<Request> actualRequest = withEmptyContext().mockRequest(new byte[0]);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();
    Object testOutput = http.with(request).returnEmpty().get();
    assertNull(testOutput);
    assertEqualRequests(request, actualRequest.get());
  }

  @Test
  public void testReadOnly() throws IOException, InterruptedException, ExecutionException {
    Supplier<Request> actualRequest = withEmptyContext().mockRequest(new byte[0]);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();
    Object testOutput = http.with(request).readOnly().returnEmpty().get();
    assertNull(testOutput);
    assertTrue(actualRequest.get().getUrl().indexOf(HttpClientImpl.PARAM_READ_ONLY_REPLICA) > -1);
  }

  @Test
  public void testHeaders() throws IOException, InterruptedException, ExecutionException {
    FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();
    headers.add("myheader1", "myvalue1");
    headers.add("myheader1", "myvalue2");
    headers.add("myheader2", "myvalue1");

    Supplier<Request> actualRequest = withContext(headers).mockRequest(new byte[0]);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").addHeader("someheader", "somevalue").build();
    http.with(request).returnEmpty().get();
    assertEqualRequests(request, actualRequest.get());
    assertFalse(actualRequest.get().getHeaders().containsKey("myheader1")); // all those headers won't be accepted, as they are not in allowed list
    assertFalse(actualRequest.get().getHeaders().containsKey("myheader2")); // all those headers won't be accepted, as they are not in allowed list
    assertTrue(actualRequest.get().getHeaders().containsKey("someheader"));
  }

  @Test
  public void testDebug() throws IOException, InterruptedException, ExecutionException {
    Request request = new RequestBuilder("GET")
        .setUrl("http://localhost/empty")
        .addHeader(X_HH_DEBUG, "true")
        .addHeader(AUTHORIZATION, "someauth")
        .build();

    // debug is off, those headers will be removed
    Supplier<Request> actualRequest = withEmptyContext().mockRequest(new byte[0]);
    assertFalse(httpClientContext.isDebugMode());
    http.with(request).returnEmpty().get();
    assertFalse(actualRequest.get().getHeaders().containsKey(X_HH_DEBUG));
    assertFalse(actualRequest.get().getHeaders().containsKey(AUTHORIZATION));

    // debug is on, headers are passed
    FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();
    headers.add(X_HH_DEBUG, "true");
    actualRequest = withContext(headers).mockRequest(new byte[0]);
    assertTrue(httpClientContext.isDebugMode());
    http.with(request).returnEmpty().get();
    assertTrue(actualRequest.get().getHeaders().containsKey(X_HH_DEBUG));
    assertTrue(actualRequest.get().getHeaders().containsKey(AUTHORIZATION));
  }

  @Test
  public void testHostsWithSession() throws IOException, InterruptedException, ExecutionException {
    FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();
    headers.add(HttpHeaders.HH_PROTO_SESSION, "somesession");

    Supplier<Request> actualRequest = withContext(headers).mockRequest(new byte[0]);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();
    http.with(request).returnEmpty().get();
    assertTrue(actualRequest.get().getHeaders().containsKey(HttpHeaders.HH_PROTO_SESSION));

    request = new RequestBuilder("GET").setUrl("http://someotherhost/empty").build();
    http.with(request).returnEmpty().get();
    assertFalse(actualRequest.get().getHeaders().containsKey(HttpHeaders.HH_PROTO_SESSION));
  }

  @Test(expected = ClientResponseException.class)
  public void testResponseError() throws Throwable {
    withEmptyContext().mockRequest(403);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();
    try {
      http.with(request).readOnly().returnEmpty().get();
    }
    catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  @Test(expected = TestException.class)
  public void testHttpClientError() throws Throwable {
    AsyncHttpClient httpClient = mock(AsyncHttpClient.class);
    when(httpClient.getConfig()).thenReturn(httpClientConfig);
    when(httpClient.executeRequest(any(), any())).then(iom -> {
      CompletionHandler handler = iom.getArgumentAt(1, CompletionHandler.class);
      handler.onThrowable(new TestException());
      return null;
    });
    http = new HttpClientBuilder(httpClient, ImmutableSet.of("localhost"), () -> httpClientContext);

    withEmptyContext();

    Request request = new RequestBuilder("GET").setUrl("http://localhost/xml").build();
    try {
      http.with(request).<ProtobufTestMessage> returnProtobuf(ProtobufTestMessage.class).get();
    }
    catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  private static class TestException extends Exception {
  }

}
