package ru.hh.jclient.common;

import com.ning.http.client.Response;

public class ResponseWrapper {
  private Response response;
  private long timeToLastByteMs;

  public ResponseWrapper(Response response, long timeToLastByteMs) {
    this.response = response;
    this.timeToLastByteMs = timeToLastByteMs;
  }

  public Response getResponse() {
    return response;
  }

  public long getTimeToLastByteMs() {
    return timeToLastByteMs;
  }
}
