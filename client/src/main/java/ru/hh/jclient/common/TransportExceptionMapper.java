package ru.hh.jclient.common;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import org.jboss.netty.channel.ConnectTimeoutException;
import com.ning.http.client.uri.Uri;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_BAD_GATEWAY;
import static ru.hh.jclient.common.ResponseStatusCodes.STATUS_CONNECT_TIMEOUT;

class TransportExceptionMapper {

  static MappedTransportErrorResponse map(Throwable t, Uri uri) {
    if (t instanceof ConnectException) {
      if (t.getCause() instanceof ConnectTimeoutException || t.getMessage().contains("time")) {
        return new MappedTransportErrorResponse(STATUS_CONNECT_TIMEOUT,
            "jclient mapped exception to status code: ConnectTimeoutException to status code", uri);
      }
      return new MappedTransportErrorResponse(STATUS_BAD_GATEWAY, "jclient mapped ConnectException to status code", uri);
    }
    if (t instanceof TimeoutException) {
      return new MappedTransportErrorResponse(STATUS_CONNECT_TIMEOUT, "jclient mapped TimeoutException to status code", uri);
    }
    return null;
  }

  private TransportExceptionMapper() {
  }
}
