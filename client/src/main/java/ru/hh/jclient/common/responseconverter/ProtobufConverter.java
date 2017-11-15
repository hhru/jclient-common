package ru.hh.jclient.common.responseconverter;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.net.MediaType.PROTOBUF;
import static java.util.Objects.requireNonNull;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.google.common.net.MediaType;
import com.google.protobuf.GeneratedMessage;

public class ProtobufConverter<T extends GeneratedMessage> extends SingleTypeConverter<T> {

  private static final Set<MediaType> MEDIA_TYPES = of(PROTOBUF.withoutParameters(), MediaType.parse("application/x-protobuf"));

  private Class<T> protobufClass;

  public ProtobufConverter(Class<T> protobufClass) {
    this.protobufClass = requireNonNull(protobufClass, "protobufClass");
  }

  @SuppressWarnings("unchecked")
  @Override
  public FailableFunction<Response, ResultWithResponse<T>, Exception> singleTypeConverterFunction() {
    return r -> {
      Method parseFromMethod = protobufClass.getMethod("parseFrom", InputStream.class);
      return new ResultWithResponse<>((T) parseFromMethod.invoke(null, r.getResponseBodyAsStream()), r);
    };
  }

  @Override
  protected Collection<MediaType> getMediaTypes() {
    return MEDIA_TYPES;
  }
}
