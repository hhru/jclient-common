package ru.hh.jclient.common.converter;

import static java.util.Objects.requireNonNull;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import ru.hh.jclient.common.ResponseWrapper;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.ning.http.client.Response;

public class PlainTextConverter implements TypeConverter<String> {

  /**
   * Default is UTF-8.
   */
  public static final Charset DEFAULT = StandardCharsets.UTF_8;

  private Charset charset;

  public PlainTextConverter(Charset charset) {
    this.charset = requireNonNull(charset, "charset must not be null");
  }

  public PlainTextConverter() {
    this(DEFAULT);
  }

  @Override
  public FailableFunction<Response, ResponseWrapper<String>, Exception> converterFunction() {
    return r -> new ResponseWrapper<>(r.getResponseBody(charset.name()), r);
  }

}
