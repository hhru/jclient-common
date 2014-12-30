package ru.hh.jclient.common;

import com.ning.http.client.Request;
import com.ning.http.client.Response;

public interface HttpRequestInfo {
  void onRequest(Request request);

  default Response onResponseReceived(Response response) {
    onResponse(response);
    return response;
  }

  void onResponse(Response response);

  default <T> T onProcessingFinished(T t) {
    onProcessingFinished();
    return t;
  }

  void onProcessingFinished();

  void addLabel(String label);

}
