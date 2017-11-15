package ru.hh.jclient.common;

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
