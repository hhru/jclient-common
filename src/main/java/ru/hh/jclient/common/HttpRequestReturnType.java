package ru.hh.jclient.common;

import java.io.IOException;
import java.util.function.Function;
import javax.xml.bind.JAXBException;
import com.google.common.net.MediaType;
import com.ning.http.client.Response;

enum HttpRequestReturnType {
  
  XML(MediaType.XML_UTF_8) {
    @SuppressWarnings("unchecked")
    @Override
    public <T> Function<Response, T> converterFunction(HttpClient context) {
      Function<Response, T> function = r -> {
        try {
          return (T) context.getJaxbContext().createUnmarshaller().unmarshal(r.getResponseBodyAsStream());
        }
        catch (JAXBException | IOException e) {
          throw new RuntimeException(e);
        }
      };
      return function;
    }
  },
  JSON(MediaType.JSON_UTF_8) {
    @Override
    public <T> Function<Response, T> converterFunction(HttpClient context) {
      // TODO implement
      return null;
    }
  },
  PROTOBUF(MediaType.PROTOBUF) {
    @Override
    public <T> Function<Response, T> converterFunction(HttpClient context) {
      // TODO implement
      return null;
    }
  },
  TEXT(MediaType.PLAIN_TEXT_UTF_8) {
    @SuppressWarnings({ "unchecked", "unused" })
    @Override
    public <T> Function<Response, T> converterFunction(HttpClient context) {
      Function<Response, T> function = r -> {
        try {
          return (T) r.getResponseBody("UTF-8");
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      };
      return function;
    }
  },
  EMPTY(MediaType.PLAIN_TEXT_UTF_8) {
    @SuppressWarnings("unused")
    @Override
    public <T> Function<Response, T> converterFunction(HttpClient context) {
      return r -> null;
    }
  };
  
  private MediaType mediaType;

  private HttpRequestReturnType(MediaType mediaType) {
    this.mediaType = mediaType;
  }
  
  public MediaType getMediaType() {
    return mediaType;
  }

  public abstract <T> Function<Response, T> converterFunction(HttpClient context);
}
