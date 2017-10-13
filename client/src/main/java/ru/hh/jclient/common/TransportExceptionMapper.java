package ru.hh.jclient.common;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import org.jboss.netty.channel.ConnectTimeoutException;
import com.ning.http.client.uri.Uri;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_BAD_GATEWAY;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_CONNECT_ERROR;

class TransportExceptionMapper {

  static MappedTransportErrorResponse map(Throwable t, Uri uri) {
    if (t instanceof ConnectException) {
      if (isConnectTimeoutError(t) || t.getMessage().contains("Connection refused")) {
        return createConnectErrorResponse(STATUS_CONNECT_ERROR, uri);
      }
      return createConnectErrorResponse(STATUS_BAD_GATEWAY, uri);
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
    return t.getCause() instanceof ConnectTimeoutException || t.getMessage().contains("time");
  }

  private TransportExceptionMapper() {
  }
}
