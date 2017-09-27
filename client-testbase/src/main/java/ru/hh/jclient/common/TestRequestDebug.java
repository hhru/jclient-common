package ru.hh.jclient.common;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ru.hh.jclient.common.exception.ResponseConverterException;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

@SuppressWarnings("unused")
public class TestRequestDebug implements RequestDebug {

  public enum Call {
    REQUEST,
    RESPONSE,
    RETRY,
    RESPONSE_CONVERTED,
    CLIENT_PROBLEM,
    CONVERTER_PROBLEM,
    FINISHED,
    LABEL
  }

  private final List<Call> calls = new ArrayList<>();
  private final boolean recordCalls;

  public TestRequestDebug() {
    this(false);
  }

  public TestRequestDebug(boolean recordCalls) {
    this.recordCalls = recordCalls;
  }

  public List<Call> getCalls() {
    return calls;
  }

  public TestRequestDebug assertCalled(Call... expectedCalls) {
    assertEquals(asList(expectedCalls), this.calls);
    reset();
    return this;
  }

  public void reset() {
    calls.clear();
  }

  @Override
  public void onRequest(AsyncHttpClientConfig config, Request request, Optional<?> requestBodyEntity) {
    record(Call.REQUEST);
  }

  @Override
  public void onRetry(AsyncHttpClientConfig config, Request request, Optional<?> requestBodyEntity, int retryCount, String upstreamName) {
    record(Call.RETRY);
  }

  @Override
  public Response onResponse(AsyncHttpClientConfig config, Response response) {
    record(Call.RESPONSE);
    return response;
  }

  @Override
  public void onResponseConverted(Optional<?> result) {
    record(Call.RESPONSE_CONVERTED);
  }

  @Override
  public void onClientProblem(Throwable t) {
    record(Call.CLIENT_PROBLEM);
  }

  @Override
  public void onConverterProblem(ResponseConverterException e) {
    record(Call.CONVERTER_PROBLEM);
  }

  @Override
  public void onProcessingFinished() {
    record(Call.FINISHED);
  }

  @Override
  public void addLabel(String label) {
    record(Call.LABEL);
  }

  private void record(Call call) {
    if (recordCalls) {
      calls.add(call);
    }
  }
}
