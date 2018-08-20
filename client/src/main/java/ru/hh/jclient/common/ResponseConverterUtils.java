package ru.hh.jclient.common;

public class ResponseConverterUtils {

  /**
   * Temporary helper until we clean-up wrapping ning to our own classes
   */
  public static Response convert(org.asynchttpclient.Response response) {
    return new Response(response);
  }

}
