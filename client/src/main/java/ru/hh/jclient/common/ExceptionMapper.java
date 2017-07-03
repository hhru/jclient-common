package ru.hh.jclient.common;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import org.jboss.netty.channel.ConnectTimeoutException;
import com.ning.http.client.uri.Uri;

class ExceptionMapper {

  private ExceptionMapper() {
  }

  static ErroneousResponse map(Throwable t, Uri uri) {
    if (t instanceof ConnectException) {
      if (t.getCause() instanceof ConnectTimeoutException || t.getMessage().contains("time")) {
        return new ErroneousResponse(504, "ConnectTimeoutException mapped to status code", uri);
      }
      return new ErroneousResponse(502, "ConnectException mapped to status code", uri);
    }
    if (t instanceof TimeoutException) {
      new ErroneousResponse(504, "TimeoutException mapped to status code", uri);
    }
    return null;
  }

}
