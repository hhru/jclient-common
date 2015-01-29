package ru.hh.jclient.common;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static ru.hh.jclient.common.HttpHeaders.X_HH_DEBUG;
import static ru.hh.jclient.common.HttpHeaders.X_REQUEST_ID;
import static ru.hh.jclient.common.TestRequestDebug.Call.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.junit.Before;
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
  private static TestRequestDebug debug = new TestRequestDebug();

  @Before
  public void before() {
    debug.reset();
  }

  private HttpClientTest withEmptyContext() {
    httpClientContext = new HttpClientContext(Collections.<String, List<String>> emptyMap(), () -> debug);
    return this;
  }

  private HttpClientTest withContext(Map<String, List<String>> headers) {
    httpClientContext = new HttpClientContext(headers, () -> new TestRequestDebug());
    return this;
  }

  private Supplier<Request> mockRequest(String text) throws IOException {
    Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(200);
    when(response.getResponseBody(isA(String.class))).thenReturn(text);
    return mockRequest(response);
  }

  private Supplier<Request> mockRequest(byte[] data) throws IOException {
    Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(200);
    when(response.getResponseBodyAsStream()).thenReturn(new ByteArrayInputStream(data));
    when(response.getResponseBodyAsBytes()).thenReturn(data);
    when(response.getResponseBody(isA(String.class))).then(iom -> {
      String charsetName = iom.getArgumentAt(0, String.class);
      return new String(data, Charset.forName(charsetName));
    });
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
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class))).then(iom -> {
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
    String text = http.with(request).returnText().get();
    assertEquals("test тест", text);
    assertEqualRequests(request, actualRequest.get());
    assertTrue(debug.called(REQUEST, RESPONSE, FINISHED));
  }

  @Test
  public void testPlainCp1251() throws InterruptedException, ExecutionException, IOException {
    Supplier<Request> actualRequest = withEmptyContext().mockRequest("test тест".getBytes(Charset.forName("Cp1251")));

    Request request = new RequestBuilder("GET").setUrl("http://localhost/plain").build();
    String text = http.with(request).returnText(Charset.forName("Cp1251")).get();
    assertEquals("test тест", text);
    assertEqualRequests(request, actualRequest.get());
    assertTrue(debug.called(REQUEST, RESPONSE, FINISHED));
  }

  @Test
  public void testResponseWrapper() throws InterruptedException, ExecutionException, IOException, JAXBException {
    XmlTest test = new XmlTest("test тест");
    ObjectMapper objectMapper = new ObjectMapper();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    objectMapper.writeValue(out, test);
    Supplier<Request> actualRequest = withEmptyContext().mockRequest(out.toByteArray());

    Request request = new RequestBuilder("GET").setUrl("http://localhost/json").build();
    ResponseWrapper<XmlTest> testOutputWrapper = http.with(request).<XmlTest> returnWrappedJson(objectMapper, XmlTest.class).get();
    XmlTest testOutput = testOutputWrapper.get();
    assertEquals(test.name, testOutput.name);
    assertNotNull(testOutputWrapper.getResponse());
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
      assertTrue(debug.called(REQUEST, RESPONSE, CONVERTER_PROBLEM, FINISHED));
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
    assertTrue(debug.called(REQUEST, RESPONSE, LABEL, FINISHED));
  }

  @Test
  public void testHeaders() throws IOException, InterruptedException, ExecutionException {
    FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();
    headers.add("myheader1", "myvalue1");
    headers.add("myheader1", "myvalue2");
    headers.add("myheader2", "myvalue1");
    headers.add(X_REQUEST_ID, "111");

    Supplier<Request> actualRequest = withContext(headers).mockRequest(new byte[0]);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").addHeader("someheader", "somevalue").build();
    http.with(request).returnEmpty().get();
    // all those headers won't be accepted, as they come from global request and are not in allowed list
    assertFalse(actualRequest.get().getHeaders().containsKey("myheader1"));
    assertFalse(actualRequest.get().getHeaders().containsKey("myheader2"));
    // this header is accepted because it consists in allowed list
    assertEquals(actualRequest.get().getHeaders().getFirstValue(X_REQUEST_ID), "111");
    // this header is accepted since it comes from local request
    assertEquals(actualRequest.get().getHeaders().getFirstValue("someheader"), "somevalue");
  }

  @Test
  public void testDebug() throws IOException, InterruptedException, ExecutionException {
    Request request = new RequestBuilder("GET")
        .setUrl("http://localhost/empty")
        .addHeader(X_HH_DEBUG, "true")
        .addHeader(AUTHORIZATION, "someauth")
        .build();

    // debug is off, headers will be removed
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
    assertEquals(actualRequest.get().getHeaders().getFirstValue(X_HH_DEBUG), "true");
    assertEquals(actualRequest.get().getHeaders().getFirstValue(AUTHORIZATION), "someauth");
  }

  @Test
  public void testHostsWithSession() throws IOException, InterruptedException, ExecutionException {
    FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();
    headers.add(HttpHeaders.HH_PROTO_SESSION, "somesession");

    Supplier<Request> actualRequest = withContext(headers).mockRequest(new byte[0]);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();
    http.with(request).returnEmpty().get();
    assertEquals(actualRequest.get().getHeaders().getFirstValue(HttpHeaders.HH_PROTO_SESSION), "somesession");

    request = new RequestBuilder("GET").setUrl("http://localhost2/empty").build();
    http.with(request).returnEmpty().get();
    assertFalse(actualRequest.get().getHeaders().containsKey(HttpHeaders.HH_PROTO_SESSION));
  }

  @Test(expected = ClientResponseException.class)
  public void testResponseError() throws Throwable {
    withEmptyContext().mockRequest(403);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();
    try {
      http.with(request).returnEmpty().get();
    }
    catch (ExecutionException e) {
      // exception about bad response status, not reported to debug, so no CLIENT_PROBLEM here
      assertTrue(debug.called(REQUEST, RESPONSE, FINISHED));
      throw e.getCause();
    }
  }

  @Test(expected = TestException.class)
  public void testHttpClientError() throws Throwable {
    AsyncHttpClient httpClient = mock(AsyncHttpClient.class);
    when(httpClient.getConfig()).thenReturn(httpClientConfig);
    when(httpClient.executeRequest(isA(Request.class), isA(CompletionHandler.class))).then(iom -> {
      CompletionHandler handler = iom.getArgumentAt(1, CompletionHandler.class);
      handler.onThrowable(new TestException());
      return null;
    });
    http = new HttpClientBuilder(httpClient, ImmutableSet.of("http://localhost"), () -> httpClientContext);

    withEmptyContext();

    Request request = new RequestBuilder("GET").setUrl("http://localhost/xml").build();
    try {
      http.with(request).<ProtobufTestMessage> returnProtobuf(ProtobufTestMessage.class).get();
    }
    catch (ExecutionException e) {
      assertTrue(debug.called(REQUEST, CLIENT_PROBLEM, FINISHED));
      throw e.getCause();
    }
  }

  private static class TestException extends Exception {
  }

}
