package ru.hh.jclient.common;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import org.jboss.netty.channel.ConnectTimeoutException;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_BAD_GATEWAY;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_CONNECT_ERROR;

class TransportExceptionMapper {

  static MappedTransportErrorResponse map(Throwable t, Uri uri) {
    if (t instanceof ConnectException) {
      if (isConnectTimeoutError(t) || t.getMessage().toLowerCase().contains("connection refused")) {
        return createConnectErrorResponse(STATUS_CONNECT_ERROR, uri);
      }
      return createConnectErrorResponse(STATUS_BAD_GATEWAY, uri);
    }
    if (t instanceof IOException && t.getMessage().toLowerCase().contains("remotely closed")) {
      return createConnectErrorResponse(STATUS_CONNECT_ERROR, uri);
    }
    if (t instanceof TimeoutException) {
      return new MappedTransportErrorResponse(STATUS_CONNECT_ERROR, "jclient mapped TimeoutException to status code", uri);
    }
    return null;
  }

  private static MappedTransportErrorResponse createConnectErrorResponse(int statusCode, Uri uri) {
    return new MappedTransportErrorResponse(statusCode, "jclient mapped ConnectException to status code", uri);
  }

  private static boolean isConnectTimeoutError(Throwable t) {
    return t.getCause() instanceof ConnectTimeoutException || t.getMessage().toLowerCase().contains("time");
  }

  private TransportExceptionMapper() {
  }
}
