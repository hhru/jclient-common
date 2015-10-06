package ru.hh.jclient.common;

public class HttpParams {

  private HttpParams() {
  }

  public static final String READ_ONLY_REPLICA = "replicaOnlyRq";
  public static final String DEBUG = "x_hh_debug";

  public static final String getDebugValue() {
    return Long.toString(System.currentTimeMillis());
  }

}
