package ru.hh.jclient.common;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ru.hh.jclient.common.exception.ResponseConverterException;

@SuppressWarnings("unused")
public class TestRequestDebug implements RequestDebug {

  private final List<Call> calls = new ArrayList<>();
  private final boolean recordCalls;
  private final boolean canUnwrapDebugResponse;

  public TestRequestDebug() {
    this(false);
  }

  public TestRequestDebug(boolean recordCalls) {
    this(recordCalls, false);
  }

  public TestRequestDebug(boolean recordCalls, boolean canUnwrapDebugResponse) {
    this.recordCalls = recordCalls;
    this.canUnwrapDebugResponse = canUnwrapDebugResponse;
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
  public void onRequest(ru.hh.jclient.common.Request request, Optional<?> requestBodyEntity, RequestContext context) {
    record(Call.REQUEST);
  }

  @Override
  public void onRetry(ru.hh.jclient.common.Request request, Optional<?> requestBodyEntity, int retryCount, RequestContext context) {
    record(Call.RETRY);
  }

  @Override
  public ru.hh.jclient.common.Response onResponse(ru.hh.jclient.common.Response response) {
    record(Call.RESPONSE);
    return response;
  }

  @Override
  public boolean canUnwrapDebugResponse() {
    return canUnwrapDebugResponse;
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

  private void record(Call call) {
    if (recordCalls) {
      calls.add(call);
    }
  }

  public enum Call {
    REQUEST, RESPONSE, RETRY, RESPONSE_CONVERTED, CLIENT_PROBLEM, CONVERTER_PROBLEM, FINISHED
  }
}
