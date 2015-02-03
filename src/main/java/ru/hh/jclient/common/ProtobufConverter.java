package ru.hh.jclient.common;

import java.io.InputStream;
import java.lang.reflect.Method;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.google.protobuf.GeneratedMessage;
import com.ning.http.client.Response;

public class ProtobufConverter<T extends GeneratedMessage> extends AbstractConverter<T> {

  private Class<T> protobufClass;

  public ProtobufConverter(Class<T> protobufClass) {
    this.protobufClass = protobufClass;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected FailableFunction<Response, ResponseWrapper<T>, Exception> converterFunction() {
    return r -> {
      Method parseFromMethod = protobufClass.getMethod("parseFrom", InputStream.class);
      return new ResponseWrapper<>((T) parseFromMethod.invoke(null, r.getResponseBodyAsStream()), r);
    };
  }

}
