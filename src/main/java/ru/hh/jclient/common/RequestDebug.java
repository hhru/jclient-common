package ru.hh.jclient.common;

import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

public interface RequestDebug {
  void onRequest(AsyncHttpClientConfig config, Request request);

  default Response onResponseReceived(Response response) {
    onResponse(response);
    return response;
  }

  void onResponse(Response response);

  void onClientProblem(Throwable t);

  void onConvertorProblem(RuntimeException e);

  default <T> T onProcessingFinished(T t) {
    onProcessingFinished();
    return t;
  }

  void onProcessingFinished();

  void addLabel(String label);

}
