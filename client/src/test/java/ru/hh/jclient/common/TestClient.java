package ru.hh.jclient.common;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestClient extends ConfigurableJClientBase<TestClient> {
  static final String TEST_UPSTREAM = "backend";
  private final ObjectMapper objectMapper = new ObjectMapper();

  TestClient(HttpClientFactory http, String upstream) {
    super("http://" + upstream, http);
  }

  public ResultWithStatus<TestDto> getData(int id) {
    RequestBuilder requestBuilder = get(jerseyUrl("url"));
    requestBuilder.addQueryParam("id", String.valueOf(id));

    return getHttp().with(requestBuilder.build()).expectJson(objectMapper, TestDto.class).resultWithStatus();
  }

  public ResultOrErrorWithStatus<TestDto, TestErrorDto> getDataOrDie(int id) {
    RequestBuilder requestBuilder = get(jerseyUrl("url"));
    requestBuilder.addQueryParam("id", String.valueOf(id));

    return getHttp().with(requestBuilder.build()).expectJson(objectMapper, TestDto.class).orJsonError(
        objectMapper,
        TestErrorDto.class
    ).resultWithStatus();
  }

  public EmptyOrErrorWithStatus<TestErrorDto> deleteData(int id) {
    RequestBuilder requestBuilder = post(jerseyUrl("url"));
    requestBuilder.addQueryParam("id", String.valueOf(id));

    return getHttp().with(requestBuilder.build()).expectNoContent().orJsonError(
        objectMapper,
        TestErrorDto.class
    ).emptyWithStatus();
  }

  public EmptyWithStatus reportDeleted(int id) {
    RequestBuilder requestBuilder = post(jerseyUrl("url"));
    requestBuilder.addQueryParam("id", String.valueOf(id));

    return getHttp().with(requestBuilder.build()).expectNoContent().emptyWithStatus();
  }

  @Override
  protected TestClient createCustomizedCopy(HttpClientFactoryConfigurator configurator) {
    return new TestClient(configurator.configure(getHttp()), TEST_UPSTREAM);
  }
}
