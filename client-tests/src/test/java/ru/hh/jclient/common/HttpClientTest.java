package ru.hh.jclient.common;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.MediaType.ANY_VIDEO_TYPE;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;
import static com.google.common.net.MediaType.PROTOBUF;
import static com.google.common.net.MediaType.XML_UTF_8;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.hh.jclient.common.HttpHeaderNames.X_HH_DEBUG;
import static ru.hh.jclient.common.HttpHeaderNames.X_REQUEST_ID;
import static ru.hh.jclient.common.HttpParams.DEBUG;
import static ru.hh.jclient.common.TestRequestDebug.Call.CLIENT_PROBLEM;
import static ru.hh.jclient.common.TestRequestDebug.Call.CONVERTER_PROBLEM;
import static ru.hh.jclient.common.TestRequestDebug.Call.FINISHED;
import static ru.hh.jclient.common.TestRequestDebug.Call.REQUEST;
import static ru.hh.jclient.common.TestRequestDebug.Call.RESPONSE;
import static ru.hh.jclient.common.TestRequestDebug.Call.RESPONSE_CONVERTED;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.type.TypeReference;
import org.asynchttpclient.AsyncHttpClient;
import org.junit.Before;
import org.junit.Test;

import ru.hh.jclient.common.HttpClientImpl.CompletionHandler;
import ru.hh.jclient.common.exception.ClientResponseException;
import ru.hh.jclient.common.exception.NoContentTypeException;
import ru.hh.jclient.common.exception.ResponseConverterException;
import ru.hh.jclient.common.exception.UnexpectedContentTypeException;
import ru.hh.jclient.common.model.JsonTest;
import ru.hh.jclient.common.model.ProtobufTest;
import ru.hh.jclient.common.model.ProtobufTest.ProtobufTestMessage;
import ru.hh.jclient.common.model.XmlError;
import ru.hh.jclient.common.model.XmlTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;

public class HttpClientTest extends HttpClientTestBase {

  private ObjectMapper objectMapper = new ObjectMapper();
  private JAXBContext jaxbContext;

  public HttpClientTest() throws JAXBException {
    jaxbContext = JAXBContext.newInstance(XmlTest.class, XmlError.class);
  }

  @Before
  public void before() {
    debug.reset();
  }

  @Test
  public void testPlain() throws InterruptedException, ExecutionException {
    Supplier<Request> actualRequest = withEmptyContext().okRequest("test тест", PLAIN_TEXT_UTF_8);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/plain").build();
    String text = http.with(request).expectPlainText().result().get();
    assertEquals("test тест", text);
    assertEqualRequests(request, actualRequest.get());
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void testPlainCp1251() throws InterruptedException, ExecutionException {
    Charset charset = Charset.forName("Cp1251");
    Supplier<Request> actualRequest = withEmptyContext().okRequest("test тест".getBytes(charset), PLAIN_TEXT_UTF_8.withCharset(charset));

    Request request = new RequestBuilder("GET").setUrl("http://localhost/plain").build();
    String text = http.with(request).expectPlainText(charset).result().get();
    assertEquals("test тест", text);
    assertEqualRequests(request, actualRequest.get());
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void testResponseWrapper() throws InterruptedException, ExecutionException, IOException {
    XmlTest test = new XmlTest("test тест");
    Supplier<Request> actualRequest = withEmptyContext().okRequest(jsonBytes(test), JSON_UTF_8);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/json").build();
    ResultWithResponse<XmlTest> testOutputWrapper = http.with(request).expectJson(objectMapper, XmlTest.class).resultWithResponse().get();
    Optional<XmlTest> testOutput = testOutputWrapper.get();
    assertEquals(test.name, testOutput.get().name);
    assertNotNull(testOutputWrapper.unconverted());
    assertEqualRequests(request, actualRequest.get());
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test(expected = NoContentTypeException.class)
  public void testNoContentType() throws Throwable {
    withEmptyContext().okRequest("test тест".getBytes(), null);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/xml").build();
    try {
      http.with(request).expectXml(jaxbContext, XmlTest.class).result().get();
    }
    catch (ExecutionException e) {
      debug.assertCalled(REQUEST, RESPONSE, FINISHED);
      throw e.getCause();
    }
  }

  @Test(expected = UnexpectedContentTypeException.class)
  public void testIncorrectContentType() throws Throwable {
    withEmptyContext().okRequest("test тест".getBytes(), JSON_UTF_8);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/xml").build();
    try {
      http.with(request).expectXml(jaxbContext, XmlTest.class).result().get();
    }
    catch (ExecutionException e) {
      debug.assertCalled(REQUEST, RESPONSE, FINISHED);
      throw e.getCause();
    }
  }

  @Test
  public void testXml() throws InterruptedException, ExecutionException, JAXBException {
    XmlTest test = new XmlTest("test тест");
    Supplier<Request> actualRequest = withEmptyContext().okRequest(xmlBytes(test), XML_UTF_8);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/xml").build();
    XmlTest testOutput = http.with(request).expectXml(jaxbContext, XmlTest.class).result().get();
    assertEquals(test.name, testOutput.name);
    assertEqualRequests(request, actualRequest.get());
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test(expected = ResponseConverterException.class)
  public void testIncorrectXml() throws Throwable {
    withEmptyContext().okRequest("test тест".getBytes(), XML_UTF_8);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/xml").build();
    try {
      http.with(request).expectXml(jaxbContext, XmlTest.class).result().get();
    }
    catch (ExecutionException e) {
      debug.assertCalled(REQUEST, RESPONSE, CONVERTER_PROBLEM, FINISHED);
      throw e.getCause();
    }
  }

  @Test
  public void testJson() throws IOException, InterruptedException, ExecutionException {
    String responseBody = "{\"name\":\"test тест\"}";
    Supplier<Request> actualRequest = withEmptyContext().okRequest(responseBody, JSON_UTF_8);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/json").build();
    XmlTest testOutput = http.with(request).expectJson(objectMapper, XmlTest.class).result().get();
    assertEquals("test тест", testOutput.name);
    assertEqualRequests(request, actualRequest.get());
  }

  @Test
  public void testJsonCollection() throws IOException, InterruptedException, ExecutionException {
    XmlTest test1 = new XmlTest("test тест1");
    XmlTest test2 = new XmlTest("test тест2");
    List<XmlTest> tests = Arrays.asList(test1, test2);
    Supplier<Request> actualRequest = withEmptyContext().okRequest(jsonBytes(tests), JSON_UTF_8);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/json").build();
    Collection<XmlTest> testOutput = http.with(request).expectJsonCollection(objectMapper, XmlTest.class).result().get();
    assertEquals(tests.size(), testOutput.size());
    assertEquals(test1.name, Iterables.get(testOutput, 0).name);
    assertEquals(test2.name, Iterables.get(testOutput, 1).name);
    assertEqualRequests(request, actualRequest.get());
  }

  @Test
  public void testJsonMap() throws IOException, InterruptedException, ExecutionException {
    XmlTest test1 = new XmlTest("test тест1");
    XmlTest test2 = new XmlTest("test тест2");
    Map<String, XmlTest> tests = Stream.of(test1, test2).collect(Collectors.toMap(xml -> xml.name, xml -> xml));
    Supplier<Request> actualRequest = withEmptyContext().okRequest(jsonBytes(tests), JSON_UTF_8);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/json").build();
    Map<String, XmlTest> testOutput = http.with(request).expectJsonMap(objectMapper, String.class, XmlTest.class).result().get();
    assertEquals(tests.size(), testOutput.size());
    assertEquals(test1, testOutput.get(test1.name));
    assertEquals(test2, testOutput.get(test2.name));
    assertEqualRequests(request, actualRequest.get());
  }

  @Test
  public void testJsonMapWithObjectKey() throws IOException, InterruptedException, ExecutionException {
    JsonTest test1 = new JsonTest(1L, "test тест1");
    JsonTest test2 = new JsonTest(2L, "test тест2");
    JsonTest test3 = new JsonTest(3L, "test тест3");
    JsonTest test4 = new JsonTest(4L, "test тест4");

    Map<JsonTest, JsonTest> testMap = new HashMap<>();
    testMap.put(test1, test2);
    testMap.put(test3, test4);

    Supplier<Request> actualRequest = withEmptyContext().okRequest(jsonBytes(testMap), JSON_UTF_8);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/json").build();
    Map<JsonTest, JsonTest> testOutput = http.with(request)
      .expectJsonMap(objectMapper, JsonTest.class, JsonTest.class).result().get();
    assertEquals(testMap.size(), testOutput.size());
    assertEquals(test2, testOutput.get(test1));
    assertEquals(test4, testOutput.get(test3));
    assertEqualRequests(request, actualRequest.get());
  }

  @Test(expected = ResponseConverterException.class)
  public void testIncorrectJson() throws Throwable {
    withEmptyContext().okRequest("test тест".getBytes(), JSON_UTF_8);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/json").build();
    try {
      http.with(request).expectJson(objectMapper, XmlTest.class).result().get();
    }
    catch (ExecutionException e) {
      debug.assertCalled(REQUEST, RESPONSE, CONVERTER_PROBLEM, FINISHED);
      throw e.getCause();
    }
  }

  @Test
  public void testProtobuf() throws IOException, InterruptedException, ExecutionException {
    ProtobufTest.ProtobufTestMessage test = ProtobufTest.ProtobufTestMessage.newBuilder().addIds(1).build();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    test.writeTo(out);

    Supplier<Request> actualRequest = withEmptyContext().okRequest(out.toByteArray(), PROTOBUF);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/protobuf").build();
    ProtobufTestMessage testOutput = http.with(request).expectProtobuf(ProtobufTestMessage.class).result().get();
    assertEquals(test.getIdsList(), testOutput.getIdsList());
    assertEqualRequests(request, actualRequest.get());
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test(expected = ResponseConverterException.class)
  public void testIncorrectProtobuf() throws Throwable {
    withEmptyContext().okRequest("test тест".getBytes(), PROTOBUF);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/protobuf").build();
    try {
      http.with(request).expectProtobuf(ProtobufTestMessage.class).result().get();
    }
    catch (ExecutionException e) {
      debug.assertCalled(REQUEST, RESPONSE, CONVERTER_PROBLEM, FINISHED);
      throw e.getCause();
    }
  }

  @Test
  public void testEmpty() throws InterruptedException, ExecutionException {
    Supplier<Request> actualRequest = withEmptyContext().okRequest(new byte[0], ANY_VIDEO_TYPE);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();
    Object testOutput = http.with(request).expectEmpty().result().get();
    assertNull(testOutput);
    assertEqualRequests(request, actualRequest.get());
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void testReadOnly() throws InterruptedException, ExecutionException {
    Supplier<Request> actualRequest = withEmptyContext().okRequest(new byte[0], ANY_VIDEO_TYPE);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();
    Object testOutput = http.with(request).readOnly().expectEmpty().result().get();
    assertNull(testOutput);
    assertTrue(actualRequest.get().getUrl().contains(HttpParams.READ_ONLY_REPLICA));
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void testHeaders() throws InterruptedException, ExecutionException {
    Map<String, List<String>> headers = new HashMap<>();
    headers.put("myheader1", Arrays.asList("myvalue1", "myvalue2"));
    headers.put("myheader2", singletonList("myvalue1"));
    headers.put(X_REQUEST_ID, singletonList("111"));

    Supplier<Request> actualRequest = withContext(headers).okRequest(new byte[0], ANY_VIDEO_TYPE);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").addHeader("someheader", "somevalue").build();
    http.with(request).expectEmpty().result().get();
    // all those headers won't be accepted, as they come from global mockRequest and are not in allowed list
    assertFalse(actualRequest.get().getHeaders().contains("myheader1"));
    assertFalse(actualRequest.get().getHeaders().contains("myheader2"));
    // this header is accepted because it consists in allowed list
    assertEquals("111", actualRequest.get().getHeaders().get(X_REQUEST_ID));
    // this header is accepted since it comes from local mockRequest
    assertEquals("somevalue", actualRequest.get().getHeaders().get("someheader"));
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test(expected = IllegalStateException.class)
  public void testDebugManualHeaderWithNoDebug() throws InterruptedException, ExecutionException {
    // situation when manually building mockRequest with debug header, it should be removed
    Request request = new RequestBuilder("GET")
        .setUrl("http://localhost/empty")
        .addHeader(X_HH_DEBUG, "true")
        .addHeader(AUTHORIZATION, "someauth")
        .build();

    withEmptyContext().okRequest(new byte[0], ANY_VIDEO_TYPE);
    assertFalse(httpClientContext.isDebugMode());
    http.with(request).expectEmpty().result().get();
  }

  @Test(expected = IllegalStateException.class)
  public void testDebugManualParamWithNoDebug() throws InterruptedException, ExecutionException {
    // situation when manually building mockRequest with debug param
    Request request = new RequestBuilder("GET")
        .setUrl("http://localhost/empty")
        .addHeader(AUTHORIZATION, "someauth")
        .addQueryParam(HttpParams.DEBUG, "123")
        .build();

    withEmptyContext().okRequest(new byte[0], ANY_VIDEO_TYPE);
    assertFalse(httpClientContext.isDebugMode());
    http.with(request).expectEmpty().result().get();
  }

  @Test
  public void testDebug() throws InterruptedException, ExecutionException {
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();
    Supplier<Request> actualRequest;

    // debug is on via header, headers are passed
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(X_HH_DEBUG, singletonList("true"));
    headers.put(AUTHORIZATION, singletonList("someauth"));

    Map<String, List<String>> queryParams = new HashMap<>();

    actualRequest = withContext(headers, queryParams).okRequest(new byte[0], ANY_VIDEO_TYPE);
    assertTrue(httpClientContext.isDebugMode());

    http.with(request).expectEmpty().result().get();

    assertEquals("true", actualRequest.get().getHeaders().get(X_HH_DEBUG));
    assertEquals("someauth", actualRequest.get().getHeaders().get(AUTHORIZATION));
    assertEquals(DEBUG, actualRequest.get().getQueryParams().get(0).getName());
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);

    // debug is on via query param, headers are passed
    headers.clear();
    headers.put(AUTHORIZATION, singletonList("someauth"));

    queryParams.clear();
    queryParams.put(DEBUG, singletonList("123"));

    actualRequest = withContext(headers, queryParams).okRequest(new byte[0], ANY_VIDEO_TYPE);
    assertTrue(httpClientContext.isDebugMode());

    http.with(request).expectEmpty().result().get();

    assertEquals("true", actualRequest.get().getHeaders().get(X_HH_DEBUG));
    assertEquals("someauth", actualRequest.get().getHeaders().get(AUTHORIZATION));
    assertEquals(DEBUG, actualRequest.get().getQueryParams().get(0).getName());
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void testExternalRequestWithDebugOn() throws InterruptedException, ExecutionException {
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();

    // debug is on but for 'external' but header / param should not be passed
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(X_HH_DEBUG, singletonList("true"));
    headers.put(AUTHORIZATION, singletonList("someauth"));

    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put(DEBUG, singletonList("123"));

    Supplier<Request> actualRequest = withContext(headers, queryParams).okRequest(new byte[0], ANY_VIDEO_TYPE);
    assertTrue(httpClientContext.isDebugMode());

    http.with(request).external().expectEmpty().result().get();

    assertFalse(actualRequest.get().getHeaders().contains(X_HH_DEBUG));
    assertFalse(actualRequest.get().getHeaders().contains(AUTHORIZATION)); // not passed through but can be added manually to mockRequest if needed
    assertTrue(actualRequest.get().getQueryParams().isEmpty());
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void testNoDebugRequestWithDebugOn() throws InterruptedException, ExecutionException {
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();

    // debug is on but for 'external' but header / param should not be passed
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(X_HH_DEBUG, singletonList("true"));
    headers.put(AUTHORIZATION, singletonList("someauth"));

    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put(DEBUG, singletonList("123"));

    Supplier<Request> actualRequest = withContext(headers, queryParams).okRequest(new byte[0], ANY_VIDEO_TYPE);
    assertTrue(httpClientContext.isDebugMode());

    http.with(request).noDebug().expectEmpty().result().get();

    assertFalse(actualRequest.get().getHeaders().contains(X_HH_DEBUG));
    assertTrue(actualRequest.get().getHeaders().contains(AUTHORIZATION)); // passed through because it might be auth not related to debug
    assertTrue(actualRequest.get().getQueryParams().isEmpty());
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void testHostsWithSession() throws InterruptedException, ExecutionException {
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HttpHeaderNames.HH_PROTO_SESSION, singletonList("somesession"));

    Supplier<Request> actualRequest = withContext(headers).okRequest(new byte[0], ANY_VIDEO_TYPE);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();
    http.with(request).expectEmpty().result().get();
    assertEquals("somesession", actualRequest.get().getHeaders().get(HttpHeaderNames.HH_PROTO_SESSION));
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);

    request = new RequestBuilder("GET").setUrl("http://localhost2/empty").build();
    http.with(request).expectEmpty().result().get();
    assertFalse(actualRequest.get().getHeaders().contains(HttpHeaderNames.HH_PROTO_SESSION));
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test(expected = ClientResponseException.class)
  public void testResponseError() throws Throwable {
    withEmptyContext().request(ANY_VIDEO_TYPE, 403);
    Request request = new RequestBuilder("GET").setUrl("http://localhost/empty").build();
    try {
      http.with(request).expectEmpty().result().get();
    }
    catch (ExecutionException e) {
      // exception about bad response status, not reported to debug, so no CLIENT_PROBLEM here
      debug.assertCalled(REQUEST, RESPONSE, FINISHED);
      throw e.getCause();
    }
  }

  @Test(expected = TestException.class)
  public void testHttpClientError() throws Throwable {
    AsyncHttpClient httpClient = mock(AsyncHttpClient.class);
    when(httpClient.getConfig()).thenReturn(httpClientConfig);
    when(httpClient.executeRequest(isA(org.asynchttpclient.Request.class), isA(CompletionHandler.class))).then(iom -> {
      CompletionHandler handler = iom.getArgument(1);
      handler.onThrowable(new TestException());
      return null;
    });
    http = createHttpClientBuilder(httpClient);
    withEmptyContext();

    Request request = new RequestBuilder("GET").setUrl("http://localhost/xml").build();
    try {
      http.with(request).expectProtobuf(ProtobufTestMessage.class).result().get();
    }
    catch (ExecutionException e) {
      debug.assertCalled(REQUEST, CLIENT_PROBLEM, FINISHED);
      throw e.getCause();
    }
  }

  @Test
  public void testErrorXml() throws InterruptedException, ExecutionException, JAXBException {
    XmlError error = new XmlError("errror message тест");
    byte[] bytes = xmlBytes(error);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/json").build();

    // with default range (>399)
    Supplier<Request> actualRequest = withEmptyContext().request(bytes, XML_UTF_8, 400);
    ResultOrErrorWithResponse<XmlTest, XmlError> response = http
        .with(request)
        .expectJson(objectMapper, XmlTest.class)
        .orXmlError(jaxbContext, XmlError.class)
        .resultWithResponse()
        .get();
    assertFalse(response.isSuccess());
    assertFalse(response.get().isPresent());
    assertTrue(response.getError().isPresent());
    assertEquals(error.message, response.getError().get().message);
    assertNotNull(response.getResponse());
    assertEqualRequests(request, actualRequest.get());
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);

    // and specific range
    actualRequest = withEmptyContext().request(bytes, XML_UTF_8, 800);

    response = http
        .with(request)
        .expectJson(objectMapper, XmlTest.class)
        .orXmlError(jaxbContext, XmlError.class)
        .forStatus(800)
        .resultWithResponse()
        .get();
    assertFalse(response.isSuccess());
    assertFalse(response.get().isPresent());
    assertTrue(response.getError().isPresent());
    assertEquals(error.message, response.getError().get().message);
    assertNotNull(response.getResponse());
    assertEquals(800, response.getResponse().getStatusCode());
    assertEqualRequests(request, actualRequest.get());
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);

    // and when range is missed
    response = http
        .with(request)
        .expectJson(objectMapper, XmlTest.class)
        .orXmlError(jaxbContext, XmlError.class)
        .forStatus(500)
        .resultWithResponse()
        .get();
    assertFalse(response.isSuccess());
    assertFalse(response.get().isPresent());
    assertFalse(response.getError().isPresent());
    assertNotNull(response.getResponse());
    assertEquals(800, response.getResponse().getStatusCode());
    assertEqualRequests(request, actualRequest.get());
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void testErrorXmlHandlesTransportError() throws Exception {
    Request request = new RequestBuilder("GET").setUrl("http://localhost/xml").build();

    MappedTransportErrorResponse errorResponse = TransportExceptionMapper.map(
      new ConnectException("test connect exception"), request.getUri());

    Supplier<Request> actualRequest = withEmptyContext().request(new Response(errorResponse));
    ResultOrErrorWithResponse<XmlTest, XmlError> response = http
      .with(request)
      .expectJson(objectMapper, XmlTest.class)
      .orXmlError(jaxbContext, XmlError.class)
      .resultWithResponse()
      .get();

    assertFalse(response.isSuccess());
    assertFalse(response.get().isPresent());
    assertFalse(response.getError().isPresent());
    assertNotNull(response.getResponse());
    assertEquals(errorResponse, response.getResponse().getDelegate());
    assertEqualRequests(request, actualRequest.get());
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void testXmlWithNoError() throws InterruptedException, ExecutionException, JAXBException {
    XmlTest test = new XmlTest("test тест");
    Supplier<Request> actualRequest = withEmptyContext().okRequest(xmlBytes(test), XML_UTF_8);

    Request request = new RequestBuilder("GET").setUrl("http://localhost/xml").build();
    ResultOrErrorWithResponse<XmlTest, String> response = http
        .with(request)
        .expectXml(jaxbContext, XmlTest.class)
        .orPlainTextError()
        .resultWithResponse()
        .get();
    assertTrue(response.get().isPresent());
    assertFalse(response.getError().isPresent());
    assertEquals(test.name, response.get().get().name);
    assertNotNull(response.getResponse());
    assertEqualRequests(request, actualRequest.get());
    debug.assertCalled(REQUEST, RESPONSE, RESPONSE_CONVERTED, FINISHED);
  }

  @Test
  public void testAcceptHeaderBeingSet() throws InterruptedException, ExecutionException, IOException, JAXBException {
    Supplier<Request> actualRequest;
    Request request = new RequestBuilder("GET").setUrl("http://localhost/content").build();

    ResultProcessor<?> resultProcessor;

    actualRequest = withEmptyContext().okRequest("test тест", PLAIN_TEXT_UTF_8);
    resultProcessor = http.with(request).expectPlainText();
    resultProcessor.result().get();
    assertProperAcceptHeader(resultProcessor, actualRequest.get());

    actualRequest = withEmptyContext().okRequest(jsonBytes(new XmlTest("zxc")), JSON_UTF_8);
    resultProcessor = http.with(request).expectJson(objectMapper, XmlTest.class);
    resultProcessor.result().get();
    assertProperAcceptHeader(resultProcessor, actualRequest.get());

    actualRequest = withEmptyContext().okRequest(xmlBytes(new XmlTest("zxc")), XML_UTF_8);
    resultProcessor = http.with(request).expectXml(jaxbContext, XmlTest.class);
    resultProcessor.result().get();
    assertProperAcceptHeader(resultProcessor, actualRequest.get());
  }

  @Test
  public void testGenericValueJsonCollection() throws IOException, ExecutionException, InterruptedException {
    var testValue = Set.of(Set.of("test"));
    Request request = new RequestBuilder("GET").setUrl("http://localhost/content").build();
    withEmptyContext().okRequest(jsonBytes(testValue), JSON_UTF_8);
    var valueType = new TypeReference<Set<String>>() {};
    Collection<Set<String>> result = http.with(request).expectJsonCollection(objectMapper, valueType).result().get();
    assertEquals(testValue.size(), result.size());
    assertEquals(testValue.stream().findFirst(), result.stream().findFirst());
  }

  @Test
  public void testGenericValueJsonMap() throws IOException, ExecutionException, InterruptedException {
    var testValue = Map.of("key", Set.of("test"));
    Request request = new RequestBuilder("GET").setUrl("http://localhost/content").build();
    withEmptyContext().okRequest(jsonBytes(testValue), JSON_UTF_8);
    var valueType = new TypeReference<Set<String>>() {};
    Map<String, Set<String>> result = http.with(request).expectJsonMap(objectMapper, String.class, valueType).result().get();
    assertEquals(testValue, result);
  }

  private static class TestException extends Exception {
  }

  private byte[] xmlBytes(Object object) throws JAXBException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    jaxbContext.createMarshaller().marshal(object, out);
    return out.toByteArray();
  }

  private byte[] jsonBytes(Object object) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    objectMapper.writeValue(out, object);
    return out.toByteArray();
  }
}
