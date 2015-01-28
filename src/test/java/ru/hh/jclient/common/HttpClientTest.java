package ru.hh.jclient.common;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.junit.Test;
import ru.hh.jclient.common.HttpClientImpl.CompletionHandler;
import ru.hh.jclient.common.model.XmlTest;
import com.google.common.collect.ImmutableSet;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
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

  private void mockRequest(String text) throws IOException {
    Response response = mock(Response.class);
    when(response.getResponseBody(any())).thenReturn(text);
    mockRequest(response);
  }

  private void mockRequest(byte[] data) throws IOException {
    Response response = mock(Response.class);
    when(response.getResponseBodyAsStream()).thenReturn(new ByteArrayInputStream(data));
    when(response.getResponseBodyAsBytes()).thenReturn(data);
    mockRequest(response);
  }

  private void mockRequest(Response response) {
    AsyncHttpClient httpClient = mock(AsyncHttpClient.class);
    when(httpClient.getConfig()).thenReturn(httpClientConfig);
    when(httpClient.executeRequest(any(), any())).then(iom -> {
      CompletionHandler handler = iom.getArgumentAt(1, CompletionHandler.class);
      handler.onCompleted(response);
      return null;
    });
    http = new HttpClientBuilder(httpClient, ImmutableSet.of("localhost"), () -> httpClientContext);
  }

  @Test
  public void testPlain() throws InterruptedException, ExecutionException, IOException {
    withEmptyContext().mockRequest("test");

    RequestBuilder request = new RequestBuilder("GET").setUrl("http://localhost/plain");
    String text = http.with(request.build()).readOnly().<String> returnText().get();
    assertEquals("test", text);
  }

  @Test
  public void testXml() throws InterruptedException, ExecutionException, IOException, JAXBException {
    XmlTest test = new XmlTest("test");
    JAXBContext context = JAXBContext.newInstance(XmlTest.class);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    context.createMarshaller().marshal(test, out);
    withEmptyContext().mockRequest(out.toByteArray());

    RequestBuilder request = new RequestBuilder("GET").setUrl("http://localhost/plain");
    XmlTest testOutput = http.with(request.build()).readOnly().<XmlTest> returnXml(context).get();
    assertEquals(test.name, testOutput.name);
  }
}
