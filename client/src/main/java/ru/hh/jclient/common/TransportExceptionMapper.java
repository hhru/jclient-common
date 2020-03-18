package ru.hh.jclient.common;

import io.netty.channel.ConnectTimeoutException;

import static java.util.Optional.ofNullable;

import static ru.hh.jclient.common.HttpStatuses.BAD_GATEWAY;
import static ru.hh.jclient.common.HttpStatuses.CONNECT_TIMEOUT_ERROR;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

final class TransportExceptionMapper {
  private static final String CONNECT_ERROR_MESSAGE = "Connect error";
  private static final String REQUEST_TIMEOUT_MESSAGE = "Request timeout";
  private static final String CONNECTION_RESET_MESSAGE = "Connection reset";

  static MappedTransportErrorResponse map(Throwable t, Uri uri) {
    final String errorMessage = ofNullable(t.getMessage()).map(String::toLowerCase).orElse("");
    if (t instanceof ConnectException) {
      if (isConnectTimeoutError(t, errorMessage) || isConnectError(errorMessage)) {
        return createErrorResponse(CONNECT_TIMEOUT_ERROR, CONNECT_ERROR_MESSAGE, uri);
      }
      return createErrorResponse(BAD_GATEWAY, toMessage(t), uri);
    }
    if (t instanceof IOException && errorMessage.contains("remotely closed")) {
      return createErrorResponse(CONNECT_TIMEOUT_ERROR, CONNECT_ERROR_MESSAGE, uri);
    }
    if (t instanceof IOException && errorMessage.contains("reset by peer")) {
      return createErrorResponse(CONNECT_TIMEOUT_ERROR, CONNECTION_RESET_MESSAGE, uri);
    }
    if (t instanceof TimeoutException) {
      return createErrorResponse(CONNECT_TIMEOUT_ERROR, REQUEST_TIMEOUT_MESSAGE, uri);
    }
    return null;
  }

  private static MappedTransportErrorResponse createErrorResponse(int statusCode, String message, Uri uri) {
    return new MappedTransportErrorResponse(statusCode, message, uri);
  }

  private static String toMessage(Throwable t) {
    return String.format("jclient mapped %s to status code", t.getClass().getName());
  }

  private static boolean isConnectTimeoutError(Throwable t, String errorMessage) {
    return t.getCause() instanceof ConnectTimeoutException || errorMessage.contains("time");
  }

  private static boolean isConnectError(String errorMessage) {
    return errorMessage.contains("connection refused")
        || errorMessage.contains("connection reset")
        || errorMessage.contains("no route to host");
  }

  private TransportExceptionMapper() {
  }
}
