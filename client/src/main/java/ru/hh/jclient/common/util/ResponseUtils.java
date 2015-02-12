package ru.hh.jclient.common.util;

import com.google.common.base.MoreObjects;
import com.ning.http.client.Response;

public class ResponseUtils {

  public static String toString(Response response) {
    return MoreObjects
        .toStringHelper(response)
        .add("uri", response.getUri())
        .add("statusCode", response.getStatusCode())
        .add("headers", response.getHeaders())
        .add("statusText", response.getStatusText())
        .toString();
  }

}
