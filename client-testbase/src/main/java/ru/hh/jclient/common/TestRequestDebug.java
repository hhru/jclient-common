package ru.hh.jclient.common;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import ru.hh.jclient.common.exception.ResponseConverterException;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

@SuppressWarnings("unused")
public class TestRequestDebug implements RequestDebug {

  public enum Call {
    REQUEST,
    RESPONSE,
    RESPONSE_CONVERTED,
    CLIENT_PROBLEM,
    CONVERTER_PROBLEM,
    FINISHED,
    LABEL
  }

  public TestRequestDebug() {
    this(false);
  }

  public TestRequestDebug(boolean recordCalls) {
    if (recordCalls) {
      calls = new HashSet<>();
    }
    else {
      calls = new HashSet<Call>() {
        @Override
        public boolean add(Call e) {
          return true;
        }
      };
    }

  }

  private Set<Call> calls = new HashSet<>();

  public Set<Call> getCalls() {
    return calls;
  }

  public TestRequestDebug assertCalled(Call... calls) {
    assertEquals(this.calls, new HashSet<>(Arrays.asList(calls)));
    reset();
    return this;
  }

  public void reset() {
    calls.clear();
  }


  @Override
  public void onRequest(AsyncHttpClientConfig config, Request request, Optional<?> requestBodyEntity) {
    calls.add(Call.REQUEST);
  }

  @Override
  public Response onResponse(AsyncHttpClientConfig config, Response response) {
    calls.add(Call.RESPONSE);
    return response;
  }

  @Override
  public Response onResponse(AsyncHttpClientConfig config, Response response, String errorMessage) {
    calls.add(Call.RESPONSE);
    return response;
  }

  @Override
  public void onResponseConverted(Optional<?> result) {
    calls.add(Call.RESPONSE_CONVERTED);
  }

  @Override
  public void onClientProblem(Throwable t) {
    calls.add(Call.CLIENT_PROBLEM);
  }

  @Override
  public void onConverterProblem(ResponseConverterException e) {
    calls.add(Call.CONVERTER_PROBLEM);
  }

  @Override
  public void onProcessingFinished() {
    calls.add(Call.FINISHED);
  }

  @Override
  public void addLabel(String label) {
    calls.add(Call.LABEL);
  }

}
