package ru.hh.jclient.common;

public class RequestResponseWrapper {
  private final Request request;
  private final Response response;
  private final long timeToLastByteMillis;

  public RequestResponseWrapper(Request request, Response response, long timeToLastByteMillis) {
    this.request = request;
    this.response = response;
    this.timeToLastByteMillis = timeToLastByteMillis;
  }

  public Request getRequest() {
    return request;
  }

  public Response getResponse() {
    return response;
  }

  public long getTimeToLastByteMillis() {
    return timeToLastByteMillis;
  }
}
