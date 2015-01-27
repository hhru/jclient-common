package ru.hh.jclient.common;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.junit.Test;
import org.mockito.Mockito;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.GeneratedMessage;
import com.ning.http.client.Response;
import ru.hh.jclient.common.exception.ResponseConverterException;
import ru.hh.jclient.common.model.ProtobufTest;
import ru.hh.jclient.common.model.XmlTest;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.ThrowableConsumer;

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

  @Test(expected = ResponseConverterException.class)
  public void testIncorrectXml() throws JAXBException, IOException {
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

  @Test(expected = ResponseConverterException.class)
  public void testIncorrectJson() throws IOException {
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

  @Test(expected = ResponseConverterException.class)
  public void testIncorrectProtobuf() throws IOException {
    InputStream in = new ByteArrayInputStream("zxc".getBytes());

    HttpClient httpClient = mock(HttpClient.class);
    Mockito.<Class<? extends GeneratedMessage>> when(httpClient.getProtobufClass()).thenReturn(ProtobufTest.ProtobufTestMessage.class);

    convertBinary(in, httpClient, ReturnType.PROTOBUF);
  }

  @Test
  public void testPlain() throws JAXBException, IOException {
    String testData = "some data с кириллицей";

    HttpClient httpClient = mock(HttpClient.class);

    Response response = mock(Response.class);
    when(response.getResponseBody("UTF-8")).thenReturn(testData);

    Function<Response, String> function = ReturnType.TEXT.converterFunction(httpClient);
    String testDataOutput = function.apply(response);

    assertEquals(testData, testDataOutput);
  }

  @Test
  public void testEmpty() throws JAXBException, IOException {
    HttpClient httpClient = mock(HttpClient.class);
    Response response = mock(Response.class);

    Function<Response, String> function = ReturnType.EMPTY.converterFunction(httpClient);
    assertNull(function.apply(response));
  }

  private <T> T convertBinary(InputStream in, HttpClient httpClient, ReturnType returnType) throws IOException {
    Response response = mock(Response.class);
    when(response.getResponseBodyAsStream()).thenReturn(in);

    Function<Response, T> function = returnType.converterFunction(httpClient);
    return function.apply(response);
  }

  private InputStream write(ThrowableConsumer<OutputStream, Exception> consumer) throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    consumer.accept(out);
    return new ByteArrayInputStream(out.toByteArray());
  }

}
