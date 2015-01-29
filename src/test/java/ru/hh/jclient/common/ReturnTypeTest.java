package ru.hh.jclient.common;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.UnmarshalException;
import org.junit.Test;
import org.mockito.Mockito;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.GeneratedMessage;
import com.ning.http.client.Response;
import ru.hh.jclient.common.model.ProtobufTest;
import ru.hh.jclient.common.model.XmlTest;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableConsumer;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;

public class ReturnTypeTest {

  @Test
  public void testXml() throws Exception {
    XmlTest testObject = new XmlTest("testName");

    JAXBContext context = JAXBContext.newInstance(XmlTest.class);
    InputStream in = write(out -> context.createMarshaller().marshal(testObject, out));

    HttpClient httpClient = mock(HttpClient.class);
    when(httpClient.getJaxbContext()).thenReturn(context);

    XmlTest testObjectOutput = convertBinary(in, httpClient, ReturnType.XML);

    assertEquals(testObject.name, testObjectOutput.name);
  }

  @Test(expected = UnmarshalException.class)
  public void testIncorrectXml() throws Exception {
    JAXBContext context = JAXBContext.newInstance(XmlTest.class);
    InputStream in = new ByteArrayInputStream("zxc".getBytes());

    HttpClient httpClient = mock(HttpClient.class);
    when(httpClient.getJaxbContext()).thenReturn(context);

    convertBinary(in, httpClient, ReturnType.XML);
  }

  @Test
  public void testJson() throws Exception {
    XmlTest testObject = new XmlTest("testName");

    ObjectMapper objectMapper = new ObjectMapper();
    InputStream in = write(out -> objectMapper.writeValue(out, testObject));

    HttpClient httpClient = mock(HttpClient.class);
    when(httpClient.getObjectMapper()).thenReturn(objectMapper);
    Mockito.<Class<?>> when(httpClient.getJsonClass()).thenReturn(XmlTest.class);

    XmlTest testObjectOutput = convertBinary(in, httpClient, ReturnType.JSON);

    assertEquals(testObject.name, testObjectOutput.name);
  }

  @Test(expected = JsonParseException.class)
  public void testIncorrectJson() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    InputStream in = new ByteArrayInputStream("zxc".getBytes());

    HttpClient httpClient = mock(HttpClient.class);
    when(httpClient.getObjectMapper()).thenReturn(objectMapper);
    Mockito.<Class<?>> when(httpClient.getJsonClass()).thenReturn(XmlTest.class);

    convertBinary(in, httpClient, ReturnType.JSON);
  }

  @Test
  public void testProtobuf() throws Exception {
    ProtobufTest.ProtobufTestMessage testObject = ProtobufTest.ProtobufTestMessage.newBuilder().addIds(1).build();

    InputStream in = write(out -> testObject.writeTo(out));

    HttpClient httpClient = mock(HttpClient.class);
    Mockito.<Class<? extends GeneratedMessage>> when(httpClient.getProtobufClass()).thenReturn(ProtobufTest.ProtobufTestMessage.class);

    ProtobufTest.ProtobufTestMessage testObjectOutput = convertBinary(in, httpClient, ReturnType.PROTOBUF);

    assertEquals(testObject.getIdsList(), testObjectOutput.getIdsList());
  }

  @Test(expected = InvocationTargetException.class)
  public void testIncorrectProtobuf() throws Exception {
    InputStream in = new ByteArrayInputStream("zxc".getBytes());

    HttpClient httpClient = mock(HttpClient.class);
    Mockito.<Class<? extends GeneratedMessage>> when(httpClient.getProtobufClass()).thenReturn(ProtobufTest.ProtobufTestMessage.class);

    convertBinary(in, httpClient, ReturnType.PROTOBUF);
  }

  @Test
  public void testPlain() throws Exception {
    String testData = "some data с кириллицей";
    testPlain(testData, StandardCharsets.UTF_8);
    testPlain(testData, Charset.forName("Cp1251"));
  }

  private void testPlain(String testData, Charset charset) throws Exception {
    byte[] data = testData.getBytes(charset);
    HttpClient httpClient = mock(HttpClient.class);
    when(httpClient.getCharset()).thenReturn(charset);

    Response response = mock(Response.class);
    when(response.getResponseBody(isA(String.class))).then(iom -> {
      String charsetName = iom.getArgumentAt(0, String.class);
      return new String(data, Charset.forName(charsetName));
    });

    FailableFunction<Response, ResponseWrapper<String>, Exception> function = ReturnType.TEXT.converterFunction(httpClient);
    String testDataOutput = function.apply(response).get();
    assertEquals(testData, testDataOutput);
  }

  @Test
  public void testEmpty() throws Exception {
    HttpClient httpClient = mock(HttpClient.class);
    Response response = mock(Response.class);

    FailableFunction<Response, ResponseWrapper<Void>, Exception> function = ReturnType.EMPTY.converterFunction(httpClient);
    assertNull(function.apply(response).get());
  }

  private <T> T convertBinary(InputStream in, HttpClient httpClient, ReturnType returnType) throws Exception {
    Response response = mock(Response.class);
    when(response.getResponseBodyAsStream()).thenReturn(in);

    FailableFunction<Response, ResponseWrapper<T>, Exception> function = returnType.converterFunction(httpClient);
    return function.apply(response).get();
  }

  private InputStream write(FailableConsumer<OutputStream, Exception> consumer) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    consumer.accept(out);
    return new ByteArrayInputStream(out.toByteArray());
  }

}
