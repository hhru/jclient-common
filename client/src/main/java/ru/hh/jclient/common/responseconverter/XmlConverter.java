package ru.hh.jclient.common.responseconverter;

import java.io.StringWriter;
import java.util.Collection;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import static java.util.Set.of;
import java.util.function.Function;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import static ru.hh.jclient.common.util.ContentType.APPLICATION_XML;
import static ru.hh.jclient.common.util.ContentType.TEXT_XML;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;

public class XmlConverter<T> extends SingleTypeConverter<T> {

  private static final Set<String> MEDIA_TYPES = of(TEXT_XML, APPLICATION_XML);
  private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

  private JAXBContext context;
  private Class<T> xmlClass;

  public XmlConverter(JAXBContext context, Class<T> xmlClass) {
    this.context = requireNonNull(context, "context must not be null");
    this.xmlClass = requireNonNull(xmlClass, "xmlClass must not be null");
  }

  @Override
  public FailableFunction<Response, ResultWithResponse<T>, Exception> singleTypeConverterFunction() {
    return r -> {
      Source source = new StreamSource(r.getResponseBodyAsStream());
      XMLStreamReader reader = XML_INPUT_FACTORY.createXMLStreamReader(source);
      JAXBElement<T> root = context.createUnmarshaller().unmarshal(reader, xmlClass);
      return new ResultWithResponse<>(root.getValue(), r);
    };
  }

  /**
   * @return function that could be used for data serialization
   */
  @Override
  public Function<T, String> reverseConverterFunction() {
    return value -> {
      try {
        Marshaller marshaller = context.createMarshaller();
        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(value, stringWriter);

        return stringWriter.toString();
      } catch (JAXBException e) {
        throw new RuntimeException(e);
      }
    };
  }

  @Override
  protected Collection<String> getContentTypes() {
    return MEDIA_TYPES;
  }
}
