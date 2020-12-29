package ru.hh.jclient.common.util;

import ru.hh.jclient.common.Response;

public class ResponseUtils {

  public static String toString(Response response) {
    return response.getClass().getSimpleName() +
           ", uri=" +
           response.getUri() +
           ", statusCode=" +
           response.getStatusCode() +
           ", headers=" +
           response.getHeaders() +
           ", statusText=" +
           response.getStatusText();
  }

}
