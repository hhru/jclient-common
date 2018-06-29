package ru.hh.jclient.common;

import static java.util.Optional.ofNullable;
import org.jboss.netty.channel.ConnectTimeoutException;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_BAD_GATEWAY;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_CONNECT_ERROR;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_REQUEST_TIMEOUT;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

final class TransportExceptionMapper {

  static MappedTransportErrorResponse map(Throwable t, Uri uri) {
    final String errorMessage = ofNullable(t.getMessage()).map(String::toLowerCase).orElse("");
    if (t instanceof ConnectException) {
      if (isConnectTimeoutError(t, errorMessage) || isConnectError(errorMessage)) {
        return createErrorResponse(STATUS_CONNECT_ERROR, t, uri);
      }
      return createErrorResponse(STATUS_BAD_GATEWAY, t, uri);
    }
    if (t instanceof IOException && errorMessage.contains("remotely closed")) {
      return createErrorResponse(STATUS_CONNECT_ERROR, t, uri);
    }
    if (t instanceof TimeoutException) {
      return createErrorResponse(STATUS_REQUEST_TIMEOUT, t, uri);
    }
    return null;
  }

  private static MappedTransportErrorResponse createErrorResponse(int statusCode, Throwable t, Uri uri) {
    return new MappedTransportErrorResponse(statusCode, String.format("jclient mapped %s to status code", t.getClass().getName()), uri);
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
