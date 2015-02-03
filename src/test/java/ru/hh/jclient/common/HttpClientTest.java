package ru.hh.jclient.common;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static ru.hh.jclient.common.HttpHeaders.X_HH_DEBUG;
import static ru.hh.jclient.common.HttpHeaders.X_REQUEST_ID;
import static ru.hh.jclient.common.TestRequestDebug.Call.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
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
import ru.hh.jclient.common.model.XmlError;
import ru.hh.jclient.common.model.XmlTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;

public class HttpClientTest extends HttpClientTestBase {

  @Before
  public void before() {
    debug.reset();
  }

  @Test
  public void testPlain() throws InterruptedException, ExecutionException, IOException {
    Supplier<Request> actualRequest = withEmptyContext().okRequest("test тест");

    Request request = new RequestBuilder("GET").setUrl("http://localhost/plain").build();
    String text = http.with(request).expectPlainText().request().get();
    assertEquals("test тест", text);
    assertEqualRequests(request, actualRequest.get());
    assertTrue(debug.called(REQUEST, RESPONSE, FINISHED));
  }

  @Test
  public void testPlainCp1251() throws InterruptedException, ExecutionException, IOException {
    Supplier<Request> actualRequest = withEmptyContext().okRequest("test тест".getBytes(Charset.forName("Cp1251")));

    Request request = new RequestBuilder("GET").setUrl("http://localhost/plain").build();
    String text = http.with(request).expectPlainText(Charset.forName("Cp1251")).request().get();
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
    Supplier<Request> actualRequest = withEmptyContext().okRequest(out.toByteArray());

    Request request = new RequestBuilder("GET").setUrl("http://localhost/json").build();
    ResponseWrapper<XmlTest> testOutputWrapper = http.with(request).expectJson(objectMapper, XmlTest.class).wrappedRequest().get();
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
    Supplier<Request> actualRequest = withEmptyContext().okRequest(out.toByteArray());

    Request request = new RequestBuilder("GET").setUrl("http://localhost/xml").build();
    XmlTest testOutput = http.with(request).expectXml(context, XmlTest.class).request().get();
    assertEquals(test.name, testOutput.name);
    assertEqualRequests(request, actualRequest.get());
  }

  @Test(expected = ResponseConverterException.class)
  public void testIncorrectXml() throws Throwable {
    withEmptyContext().okRequest("test тест".getBytes());

    Request request = new RequestBuilder("GET").setUrl("http://localhost/xml").build();
    try {
      http.with(request).expectXml(JAXBContext.newInstance(XmlTest.class), XmlTest.class).request().get();
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
    Supplier<Request> actualRequest = withEmptyContext().okRequest(out.toByteArray());

    Request request = new RequestBuilder("GET").setUrl("http://localhost/json").build();
    XmlTest testOutput = http.with(request).<XmlTest> expectJson(objectMapper, XmlTest.class).request().get();
    assertEquals(test.name, testOutput.name);
    assertEqualRequests(request, actualRequest.get());
  }

  @Test(expected = ResponseConverterException.class)
  public void testIncorrectJson() throws Throwable {
    withEmptyContext().okRequest("test тест".getBytes());

    Request request = new RequestBuilder("GET").setUrl("http://localhost/json").build();
    try {
      http.with(request).<XmlTest> expectJson(new ObjectMapper(), XmlTest.class).request().get();
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

    Supplier<Request> actualRequest = withEmptyContext().okRequest(out.toByteArray());

    Request request = new RequestBuilder("GET").setUrl("http://localhost/protobuf").build();
    ProtobufTestMessage testOutput = http.with(request).<ProtobufTestMessage> expectProtobuf(ProtobufTestMessage.class).request().get();
    assertEquals(test.getIdsList(), testOutput.getIdsList());
    assertEqualRequests(request, actualRequest.get());
  }

  @Test(expected = ResponseConverterException.class)
  public void testIncorrectProtobuf() throws Throwable {
    withEmptyContext().okRequest("test тест".getBytes());

    Request request = new RequestBuilder("GET").setUrl("http://localhost/protobuf").build();
    try {
      http.with(request).<ProtobufTestMessage> expectProtobuf(ProtobufTestMessage.class).request().get();
    }
    catch (ExecutionException e) {
      throw e.getCause();
    }
  }

  @Test
  public void testEmpty() throws IOException, InterruptedException, ExecutionException {
    Supplier<Request> actualRequest = withEmptyContext().okRequest(new byte[0]);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();
    Object testOutput = http.with(request).expectEmpty().request().get();
    assertNull(testOutput);
    assertEqualRequests(request, actualRequest.get());
  }

  @Test
  public void testReadOnly() throws IOException, InterruptedException, ExecutionException {
    Supplier<Request> actualRequest = withEmptyContext().okRequest(new byte[0]);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();
    Object testOutput = http.with(request).readOnly().expectEmpty().request().get();
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

    Supplier<Request> actualRequest = withContext(headers).okRequest(new byte[0]);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").addHeader("someheader", "somevalue").build();
    http.with(request).expectEmpty().request().get();
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
    Supplier<Request> actualRequest = withEmptyContext().okRequest(new byte[0]);
    assertFalse(httpClientContext.isDebugMode());
    http.with(request).expectEmpty().request().get();
    assertFalse(actualRequest.get().getHeaders().containsKey(X_HH_DEBUG));
    assertFalse(actualRequest.get().getHeaders().containsKey(AUTHORIZATION));

    // debug is on, headers are passed
    FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();
    headers.add(X_HH_DEBUG, "true");
    actualRequest = withContext(headers).okRequest(new byte[0]);
    assertTrue(httpClientContext.isDebugMode());
    http.with(request).expectEmpty().request().get();
    assertEquals(actualRequest.get().getHeaders().getFirstValue(X_HH_DEBUG), "true");
    assertEquals(actualRequest.get().getHeaders().getFirstValue(AUTHORIZATION), "someauth");
  }

  @Test
  public void testHostsWithSession() throws IOException, InterruptedException, ExecutionException {
    FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();
    headers.add(HttpHeaders.HH_PROTO_SESSION, "somesession");

    Supplier<Request> actualRequest = withContext(headers).okRequest(new byte[0]);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();
    http.with(request).expectEmpty().request().get();
    assertEquals(actualRequest.get().getHeaders().getFirstValue(HttpHeaders.HH_PROTO_SESSION), "somesession");

    request = new RequestBuilder("GET").setUrl("http://localhost2/empty").build();
    http.with(request).expectEmpty().request().get();
    assertFalse(actualRequest.get().getHeaders().containsKey(HttpHeaders.HH_PROTO_SESSION));
  }

  @Test(expected = ClientResponseException.class)
  public void testResponseError() throws Throwable {
    withEmptyContext().request(403);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();
    try {
      http.with(request).expectEmpty().request().get();
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
      http.with(request).<ProtobufTestMessage> expectProtobuf(ProtobufTestMessage.class).request().get();
    }
    catch (ExecutionException e) {
      assertTrue(debug.called(REQUEST, CLIENT_PROBLEM, FINISHED));
      throw e.getCause();
    }
  }

  @Test
  public void testErrorXml() throws InterruptedException, ExecutionException, IOException, JAXBException {
    XmlError error = new XmlError("errror message тест");
    JAXBContext context = JAXBContext.newInstance(XmlError.class);
    ObjectMapper objectMapper = new ObjectMapper();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    context.createMarshaller().marshal(error, out);
    Supplier<Request> actualRequest = withEmptyContext().request(out.toByteArray(), 400);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/json").build();
    ResponseAndErrorWrapper<XmlTest, XmlError> response = http
        .with(request)
        .expectJson(objectMapper, XmlTest.class)
        .orXmlError(context, XmlError.class)
        .request()
        .get();
    assertFalse(response.get().isPresent());
    assertTrue(response.getError().isPresent());
    assertEquals(error.message, response.getError().get().message);
    assertNotNull(response.getResponse());
    assertEqualRequests(request, actualRequest.get());
  }

  @Test
  public void testXmlWithNoError() throws InterruptedException, ExecutionException, IOException, JAXBException {
    XmlTest test = new XmlTest("test тест");
    JAXBContext context = JAXBContext.newInstance(XmlTest.class);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    context.createMarshaller().marshal(test, out);
    Supplier<Request> actualRequest = withEmptyContext().okRequest(out.toByteArray());

    Request request = new RequestBuilder("GET").setUrl("http://localhost/xml").build();
    ResponseAndErrorWrapper<XmlTest, String> response = http.with(request).expectXml(context, XmlTest.class).orPlainTextError().request().get();
    assertTrue(response.get().isPresent());
    assertFalse(response.getError().isPresent());
    assertEquals(test.name, response.get().get().name);
    assertNotNull(response.getResponse());
    assertEqualRequests(request, actualRequest.get());
  }

  private static class TestException extends Exception {
  }

}
