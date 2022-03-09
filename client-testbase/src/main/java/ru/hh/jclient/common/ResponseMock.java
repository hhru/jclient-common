package ru.hh.jclient.common;

public class ResponseMock extends Response {
  private int status;
  private String statusText;

  public ResponseMock(int status, String statusText) {
    this.status = status;
    this.statusText = statusText;
  }

  public static ResponseMock ok() {
    return new ResponseMock(200, null);
  }

  public static ResponseMock empty() {
    return new ResponseMock(204, null);
  }

  public void setStatusCode(int status) {
    this.status = status;
  }

  @Override
  public int getStatusCode() {
    return status;
  }

  public void setStatusText(String statusText) {
    this.statusText = statusText;
  }

  @Override
  public String getStatusText() {
    return statusText;
  }
}
