package ru.hh.jclient.common;

public class ResponseMock extends Response {
  private int status;
  private String statusText;

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
