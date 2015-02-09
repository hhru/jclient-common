package ru.hh.jclient.common.converter;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.net.MediaType.XML_UTF_8;
import static java.util.Objects.requireNonNull;
import java.util.Collection;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.google.common.net.MediaType;
import com.ning.http.client.Response;

public class XmlConverter<T> extends SingleTypeConverter<T> {

  private static final Set<MediaType> MEDIA_TYPES = of(XML_UTF_8.withoutParameters());

  private JAXBContext context;
  @SuppressWarnings("unused")
  private Class<T> xmlClass; // / used to preserve T type when chaining

  public XmlConverter(JAXBContext context, Class<T> xmlClass) {
    this.context = requireNonNull(context, "context must not be null");
    this.xmlClass = requireNonNull(xmlClass, "xmlClass must not be null");
  }

  @SuppressWarnings("unchecked")
  @Override
  public FailableFunction<Response, ResponseWrapper<T>, Exception> singleTypeConverterFunction() {
    return r -> new ResponseWrapper<>((T) context.createUnmarshaller().unmarshal(r.getResponseBodyAsStream()), r);
  }

  @Override
  public Collection<MediaType> getMediaTypes() {
    return MEDIA_TYPES;
  }
}
