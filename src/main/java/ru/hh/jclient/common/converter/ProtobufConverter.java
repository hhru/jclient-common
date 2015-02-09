package ru.hh.jclient.common.converter;

import static java.util.Objects.requireNonNull;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import com.google.protobuf.GeneratedMessage;
import com.ning.http.client.Response;

public class ProtobufConverter<T extends GeneratedMessage> extends SingleTypeConverter<T> {

  private Class<T> protobufClass;

  public ProtobufConverter(Class<T> protobufClass) {
    this.protobufClass = requireNonNull(protobufClass, "protobufClass");
  }

  @SuppressWarnings("unchecked")
  @Override
  public FailableFunction<Response, ResponseWrapper<T>, Exception> singleTypeConverterFunction() {
    return r -> {
      Method parseFromMethod = protobufClass.getMethod("parseFrom", InputStream.class);
      return new ResponseWrapper<>((T) parseFromMethod.invoke(null, r.getResponseBodyAsStream()), r);
    };
  }

  @Override
  public Collection<MediaType> getMediaTypes() {
    return ImmutableSet.of(MediaType.PROTOBUF.withoutParameters(), MediaType.parse("application/x-protobuf"));
  }
}
