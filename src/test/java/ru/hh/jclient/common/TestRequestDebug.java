package ru.hh.jclient.common;

import ru.hh.jclient.common.exception.ResponseConverterException;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

@SuppressWarnings("unused")
public class TestRequestDebug implements RequestDebug {

  @Override
  public void onRequest(AsyncHttpClientConfig config, Request request) {
  }

  @Override
  public Response onResponse(AsyncHttpClientConfig config, Response response) {
    return response;
  }

  @Override
  public void onClientProblem(Throwable t) {
  }

  @Override
  public void onConverterProblem(ResponseConverterException e) {
  }

  @Override
  public void onProcessingFinished() {
  }

  @Override
  public void addLabel(String label) {
  }

}
