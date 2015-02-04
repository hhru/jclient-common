package ru.hh.jclient.common.converter;

import static java.util.Objects.requireNonNull;
import javax.xml.bind.JAXBContext;
import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.ning.http.client.Response;

public class XmlConverter<T> implements TypeConverter<T> {

  private JAXBContext context;
  @SuppressWarnings("unused")
  private Class<T> xmlClass; // / used to preserve T type when chaining

  public XmlConverter(JAXBContext context, Class<T> xmlClass) {
    this.context = requireNonNull(context, "context must not be null");
    this.xmlClass = requireNonNull(xmlClass, "xmlClass must not be null");
  }

  @SuppressWarnings("unchecked")
  @Override
  public FailableFunction<Response, ResponseWrapper<T>, Exception> converterFunction() {
    return r -> new ResponseWrapper<>((T) context.createUnmarshaller().unmarshal(r.getResponseBodyAsStream()), r);
  }

}
