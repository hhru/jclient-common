package ru.hh.jclient.errors.impl;

public class HttpStatuses {

  // jersey1 does not have those in Response.Status enum
  public static final int BAD_GATEWAY = 502;
  public static final int GATEWAY_TIMEOUT = 504;

}
