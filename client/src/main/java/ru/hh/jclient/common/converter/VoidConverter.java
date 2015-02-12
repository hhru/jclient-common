package ru.hh.jclient.common.converter;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.net.MediaType.ANY_TYPE;
import java.util.Collection;
import java.util.Set;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.google.common.net.MediaType;
import com.ning.http.client.Response;

public class VoidConverter extends SingleTypeConverter<Void> {

  private static final Set<MediaType> MEDIA_TYPES = of(ANY_TYPE);

  @Override
  public FailableFunction<Response, ResultWithResponse<Void>, Exception> singleTypeConverterFunction() {
    return r -> new ResultWithResponse<>(null, r);
  }

  @Override
  public Collection<MediaType> getMediaTypes() {
    return MEDIA_TYPES;
  }
}
