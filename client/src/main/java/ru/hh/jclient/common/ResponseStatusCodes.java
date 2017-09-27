package ru.hh.jclient.common;

public class ResponseStatusCodes {
  public static final int STATUS_BAD_GATEWAY = 502;
  public static final int STATUS_REQUEST_TIMEOUT = 503;
  public static final int STATUS_CONNECT_TIMEOUT = 599;

  private ResponseStatusCodes() {
  }
}
