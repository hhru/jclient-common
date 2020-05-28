package ru.hh.jclient.common;

public class ResponseWrapper {
  private Response response;
  private long timeToLastByteMicros;

  public ResponseWrapper(Response response, long timeToLastByteMicros) {
    this.response = response;
    this.timeToLastByteMicros = timeToLastByteMicros;
  }

  public Response getResponse() {
    return response;
  }

  public long getTimeToLastByteMicros() {
    return timeToLastByteMicros;
  }
}
