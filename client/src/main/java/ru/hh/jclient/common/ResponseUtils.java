package ru.hh.jclient.common;

public class ResponseUtils {

  /**
   * Temporary helper until we clean-up wrapping ning to our own classes
   */
  public static Response convert(com.ning.http.client.Response response) {
    return new Response(response);
  }

}
