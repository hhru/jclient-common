package ru.hh.jclient.common;

import java.io.InputStream;
import java.lang.reflect.Method;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.google.common.net.MediaType;
import com.ning.http.client.Response;

enum ReturnType {
  
  XML(MediaType.XML_UTF_8) {
    @SuppressWarnings("unchecked")
    @Override
    <T> FailableFunction<Response, T, Exception> converterFunction(HttpClient context) {
      return r -> (T) context.getJaxbContext().createUnmarshaller().unmarshal(r.getResponseBodyAsStream());
    }
  },
  JSON(MediaType.JSON_UTF_8) {
    @SuppressWarnings("unchecked")
    @Override
    <T> FailableFunction<Response, T, Exception> converterFunction(HttpClient context) {
      return r -> (T) context.getObjectMapper().readValue(r.getResponseBodyAsStream(), context.getJsonClass());
    }
  },
  PROTOBUF(MediaType.PROTOBUF) {
    @SuppressWarnings("unchecked")
    @Override
    <T> FailableFunction<Response, T, Exception> converterFunction(HttpClient context) {
      return r -> {
        Method parseFromMethod = context.getProtobufClass().getMethod("parseFrom", InputStream.class);
        return (T) parseFromMethod.invoke(null, r.getResponseBodyAsStream());
      };
    }
  },
  TEXT(MediaType.PLAIN_TEXT_UTF_8) {
    @SuppressWarnings({ "unchecked", "unused" })
    @Override
    <T> FailableFunction<Response, T, Exception> converterFunction(HttpClient context) {
      return r -> (T) r.getResponseBody("UTF-8");
    }
  },
  EMPTY(MediaType.PLAIN_TEXT_UTF_8) {
    @SuppressWarnings("unused")
    @Override
    <T> FailableFunction<Response, T, Exception> converterFunction(HttpClient context) {
      return r -> null;
    }
  };
  
  private MediaType mediaType;

  private ReturnType(MediaType mediaType) {
    this.mediaType = mediaType;
  }
  
  public MediaType getMediaType() {
    return mediaType;
  }

  abstract <T> FailableFunction<Response, T, Exception> converterFunction(HttpClient context);
}
