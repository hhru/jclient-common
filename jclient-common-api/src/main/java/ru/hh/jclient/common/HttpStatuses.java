package ru.hh.jclient.common;

public interface HttpStatuses {
  int SC_OK = 200;
  int SC_CREATED = 201;
  int SC_NO_CONTENT = 204;
  int SC_BAD_REQUEST = 400;

  int BAD_GATEWAY = 502;
  int SERVICE_UNAVAILABLE = 503;
  int GATEWAY_TIMEOUT = 504;

  int CONNECT_TIMEOUT_ERROR = 599;

  /**
   * use CONNECT_TIMEOUT_ERROR
   */
  @Deprecated
  int CONNECT_ERROR = 599;

  /**
   * use CONNECT_TIMEOUT_ERROR
   */
  @Deprecated
  int REQUEST_TIMEOUT = 599;
}
