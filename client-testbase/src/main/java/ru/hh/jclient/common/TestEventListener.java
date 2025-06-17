package ru.hh.jclient.common;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import ru.hh.jclient.common.exception.ResponseConverterException;

@SuppressWarnings("unused")
public class TestEventListener implements HttpClientEventListener {

  private final List<Call> calls = new ArrayList<>();
  private final boolean recordCalls;
  private final boolean canUnwrapDebugResponse;

  public TestEventListener() {
    this(false);
  }

  public TestEventListener(boolean recordCalls) {
    this(recordCalls, false);
  }

  public TestEventListener(boolean recordCalls, boolean canUnwrapDebugResponse) {
    this.recordCalls = recordCalls;
    this.canUnwrapDebugResponse = canUnwrapDebugResponse;
  }

  public List<Call> getCalls() {
    return calls;
  }

  public TestEventListener assertCalled(Call... expectedCalls) {
    assertEquals(asList(expectedCalls), this.calls);
    reset();
    return this;
  }

  public void reset() {
    calls.clear();
  }

  @Override
  public void onRequest(Request request, @Nullable Object requestBodyEntity, RequestContext context) {
    record(Call.REQUEST);
  }

  @Override
  public void onRetry(Request request, @Nullable Object requestBodyEntity, int retryCount, RequestContext context) {
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
  public void onResponseConverted(@Nullable Object result) {
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

