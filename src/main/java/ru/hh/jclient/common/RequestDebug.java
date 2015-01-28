package ru.hh.jclient.common;

import ru.hh.jclient.common.exception.ResponseConverterException;

import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

public interface RequestDebug {
  void onRequest(AsyncHttpClientConfig config, Request request);

  Response onResponse(AsyncHttpClientConfig config, Response response);

  void onClientProblem(Throwable t);

  void onConverterProblem(ResponseConverterException e);

  void onProcessingFinished();

  void addLabel(String label);

}
