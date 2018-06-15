package ru.hh.jclient.common;

import static java.util.Optional.ofNullable;
import org.jboss.netty.channel.ConnectTimeoutException;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_BAD_GATEWAY;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_CONNECT_ERROR;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

class TransportExceptionMapper {

  static MappedTransportErrorResponse map(Throwable t, Uri uri) {
    final String errorMessage = ofNullable(t.getMessage()).map(String::toLowerCase).orElse("");
    if (t instanceof ConnectException) {
      if (isConnectTimeoutError(t, errorMessage) || isConnectError(errorMessage)) {
        return createConnectErrorResponse(STATUS_CONNECT_ERROR, uri);
      }
      return createConnectErrorResponse(STATUS_BAD_GATEWAY, uri);
    }
    if (t instanceof IOException && errorMessage.contains("remotely closed")) {
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

  private static boolean isConnectTimeoutError(Throwable t, String errorMessage) {
    return t.getCause() instanceof ConnectTimeoutException || errorMessage.contains("time");
  }

  private static boolean isConnectError(String errorMessage) {
    return errorMessage.contains("connection refused") || errorMessage.contains("connection reset");
  }

  private TransportExceptionMapper() {
  }
}
