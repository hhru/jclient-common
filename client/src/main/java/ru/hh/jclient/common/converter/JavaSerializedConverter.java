package ru.hh.jclient.common.converter;

import com.google.common.net.MediaType;
import com.ning.http.client.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Collection;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;

public class JavaSerializedConverter<T> extends SingleTypeConverter<T> {
  private static final Set<MediaType> MEDIA_TYPES = of(MediaType.parse("application/x-java-serialized-object"));

  private final Class<T> clazz;
  private boolean isRootClassResolved;

  public JavaSerializedConverter(Class<T> clazz) {
    this.clazz = clazz;
  }

  @SuppressWarnings("unchecked")
  @Override
  public FailableFunction<Response, ResultWithResponse<T>, Exception> singleTypeConverterFunction() {
    return r -> {
      InputStream stream = r.getResponseBodyAsStream();
      if (stream == null) {
        return new ResultWithResponse<>(null, r);
      }

      try (ClassAwareObjectInputStream in = new ClassAwareObjectInputStream(stream)) {
        return new ResultWithResponse<>((T) in.readObject(), r);
      }
    };
  }

  @Override
  protected Collection<MediaType> getMediaTypes() {
    return MEDIA_TYPES;
  }

  private class ClassAwareObjectInputStream extends ObjectInputStream {

    ClassAwareObjectInputStream(InputStream in) throws IOException {
      super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      Class<?> resolvedClass = super.resolveClass(desc);
      if (isRootClassResolved) {
        return resolvedClass;
      }
      isRootClassResolved = true;

      if (!clazz.isAssignableFrom(resolvedClass)) {
        throw new InvalidClassException("failed to deserialize object, expected: " + clazz, ", actual: " + resolvedClass);
      }
      return resolvedClass;
    }
  }
}
