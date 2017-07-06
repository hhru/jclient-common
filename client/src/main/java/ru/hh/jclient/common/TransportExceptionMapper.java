package ru.hh.jclient.common;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import org.jboss.netty.channel.ConnectTimeoutException;
import com.ning.http.client.uri.Uri;

class TransportExceptionMapper {

  private TransportExceptionMapper() {
  }

  static MappedTransportErrorResponse map(Throwable t, Uri uri) {
    if (t instanceof ConnectException) {
      if (t.getCause() instanceof ConnectTimeoutException || t.getMessage().contains("time")) {
        return new MappedTransportErrorResponse(504, "jclient mapped exception to status code: ConnectTimeoutException to status code", uri);
      }
      return new MappedTransportErrorResponse(502, "jclient mapped ConnectException to status code", uri);
    }
    if (t instanceof TimeoutException) {
      new MappedTransportErrorResponse(504, "jclient mapped TimeoutException to status code", uri);
    }
    return null;
  }

}
