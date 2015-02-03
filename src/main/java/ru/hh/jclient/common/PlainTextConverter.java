package ru.hh.jclient.common;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.charset.Charset;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.ning.http.client.Response;

public class PlainTextConverter extends AbstractConverter<String> {

  private Charset charset;

  public PlainTextConverter(Charset charset) {
    this.charset = charset;
  }

  public PlainTextConverter() {
    this(UTF_8);
  }

  @Override
  protected FailableFunction<Response, ResponseWrapper<String>, Exception> converterFunction() {
    return r -> new ResponseWrapper<>(r.getResponseBody(charset.name()), r);
  }

}
