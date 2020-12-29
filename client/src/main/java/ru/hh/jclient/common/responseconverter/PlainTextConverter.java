package ru.hh.jclient.common.responseconverter;

import static java.util.Objects.requireNonNull;
import static ru.hh.jclient.common.util.ContentType.TEXT_PLAIN;
import static ru.hh.jclient.common.util.ContentType.WITH_CHARSET;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;
import ru.hh.jclient.common.Response;
import ru.hh.jclient.common.ResultWithResponse;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;

public class PlainTextConverter extends SingleTypeConverter<String> {

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
  public FailableFunction<Response, ResultWithResponse<String>, Exception> singleTypeConverterFunction() {
    return r -> new ResultWithResponse<>(r.getResponseBody(charset), r);
  }

  @Override
  protected Collection<String> getMediaTypes() {
    return Set.of(TEXT_PLAIN + WITH_CHARSET + charset.name());
  }
}
