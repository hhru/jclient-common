package ru.hh.jclient.common;

import javax.xml.bind.JAXBContext;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.ning.http.client.Response;

public class XmlProcessor<T> extends AbstractProcessor<T> {

  private JAXBContext context;
  @SuppressWarnings("unused")
  private Class<T> xmlClass;

  public XmlProcessor(HttpClient httpClient, JAXBContext context, Class<T> xmlClass) {
    super(httpClient);
    this.context = context;
    this.xmlClass = xmlClass;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected FailableFunction<Response, ResponseWrapper<T>, Exception> converterFunction() {
    return r -> new ResponseWrapper<>((T) context.createUnmarshaller().unmarshal(r.getResponseBodyAsStream()), r);
  }

}
