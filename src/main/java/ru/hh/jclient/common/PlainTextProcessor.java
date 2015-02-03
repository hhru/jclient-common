package ru.hh.jclient.common;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.charset.Charset;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.ning.http.client.Response;

public class PlainTextProcessor extends AbstractProcessor<String> {

  private Charset charset;

  public PlainTextProcessor(HttpClient httpClient, Charset charset) {
    super(httpClient);
    this.charset = charset;
  }

  public PlainTextProcessor(HttpClient httpClient) {
    this(httpClient, UTF_8);
  }

  @Override
  protected FailableFunction<Response, ResponseWrapper<String>, Exception> converterFunction() {
    return r -> new ResponseWrapper<>(r.getResponseBody(charset.name()), r);
  }

}
