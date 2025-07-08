package ru.hh.jclient.common;

import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.socket.ChannelOutputShutdownException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import java.util.concurrent.TimeoutException;
import ru.hh.deadline.context.DeadlineContext;
import static ru.hh.jclient.common.HttpStatuses.BAD_GATEWAY;
import static ru.hh.jclient.common.HttpStatuses.CONNECT_TIMEOUT_ERROR;
import static ru.hh.jclient.common.HttpStatuses.INSUFFICIENT_TIMEOUT;
import static ru.hh.jclient.common.HttpStatuses.SERVER_TIMEOUT;
import static ru.hh.jclient.common.ResponseStatusMessages.CHANNEL_CLOSED_MESSAGE;
import static ru.hh.jclient.common.ResponseStatusMessages.CHANNEL_OUTPUT_SHUTDOWN_MESSAGE;
import static ru.hh.jclient.common.ResponseStatusMessages.CONNECTION_CLOSED_MESSAGE;
import static ru.hh.jclient.common.ResponseStatusMessages.CONNECTION_RESET_MESSAGE;
import static ru.hh.jclient.common.ResponseStatusMessages.CONNECT_ERROR_MESSAGE;
import static ru.hh.jclient.common.ResponseStatusMessages.INSUFFICIENT_TIMEOUT_MESSAGE;
import static ru.hh.jclient.common.ResponseStatusMessages.SERVER_TIMEOUT_MESSAGE;

final class TransportExceptionMapper {
  static MappedTransportErrorResponse map(Throwable t, Uri uri, DeadlineContext deadlineContext) {
    final String lowerCasedErrorMessage = ofNullable(t.getMessage()).map(String::toLowerCase).orElse("");
    if (t instanceof ConnectException) {
      if (isConnectTimeoutError(t, lowerCasedErrorMessage) || isConnectError(lowerCasedErrorMessage)) {
        return createErrorResponse(CONNECT_TIMEOUT_ERROR, CONNECT_ERROR_MESSAGE, uri);
      }
      return createErrorResponse(BAD_GATEWAY, toMessage(t), uri);
    }
    if (t instanceof SocketException && lowerCasedErrorMessage.contains("connection reset")) {
      return createErrorResponse(CONNECT_TIMEOUT_ERROR, CONNECTION_RESET_MESSAGE, uri);
    }
    if (t instanceof IOException && lowerCasedErrorMessage.contains("remotely closed")) {
      return createErrorResponse(CONNECT_TIMEOUT_ERROR, CONNECTION_CLOSED_MESSAGE, uri);
    }
    if (t instanceof IOException && lowerCasedErrorMessage.contains("reset by peer")) {
      return createErrorResponse(CONNECT_TIMEOUT_ERROR, CONNECTION_RESET_MESSAGE, uri);
    }
    if (t instanceof TimeoutException) {
      if (isNull(deadlineContext) || deadlineContext.isDeadlineCorrect()) {
        return createErrorResponse(SERVER_TIMEOUT, SERVER_TIMEOUT_MESSAGE, uri);
      } else {
        return createErrorResponse(INSUFFICIENT_TIMEOUT, INSUFFICIENT_TIMEOUT_MESSAGE, uri);
      }
    }
    if (t instanceof ClosedChannelException) {
      return createErrorResponse(CONNECT_TIMEOUT_ERROR, CHANNEL_CLOSED_MESSAGE, uri);
    }
    if (t instanceof ChannelOutputShutdownException) {
      return createErrorResponse(CONNECT_TIMEOUT_ERROR, CHANNEL_OUTPUT_SHUTDOWN_MESSAGE, uri);
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

  private static boolean isConnectError(String lowerCasedErrorMessage) {
    return lowerCasedErrorMessage.contains("connection refused")
        || lowerCasedErrorMessage.contains("connection reset")
        || lowerCasedErrorMessage.contains("no route to host");
  }

  private TransportExceptionMapper() {
  }
}
