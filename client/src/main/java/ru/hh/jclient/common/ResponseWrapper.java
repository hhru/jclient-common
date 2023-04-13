package ru.hh.jclient.common;

public class ResponseWrapper {
  private Response response;
  private long timeToLastByteMillis;

  public ResponseWrapper(Response response, long timeToLastByteMillis) {
    this.response = response;
    this.timeToLastByteMillis = timeToLastByteMillis;
  }

  public Response getResponse() {
    return response;
  }

  public long getTimeToLastByteMillis() {
    return timeToLastByteMillis;
  }
}
