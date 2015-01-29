package ru.hh.jclient.common;

import java.io.InputStream;
import java.lang.reflect.Method;
import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;
import com.ning.http.client.Response;

enum ReturnType {
  
  XML {
    @SuppressWarnings("unchecked")
    @Override
    <T> FailableFunction<Response, ResponseWrapper<T>, Exception> converterFunction(HttpClient context) {
      return r -> new ResponseWrapper<T>((T) context.getJaxbContext().createUnmarshaller().unmarshal(r.getResponseBodyAsStream()), r);
    }
  },
  JSON {
    @SuppressWarnings("unchecked")
    @Override
    <T> FailableFunction<Response, ResponseWrapper<T>, Exception> converterFunction(HttpClient context) {
      return r -> new ResponseWrapper<T>((T) context.getObjectMapper().readValue(r.getResponseBodyAsStream(), context.getJsonClass()), r);
    }
  },
  PROTOBUF {
    @SuppressWarnings("unchecked")
    @Override
    <T> FailableFunction<Response, ResponseWrapper<T>, Exception> converterFunction(HttpClient context) {
      return r -> {
        Method parseFromMethod = context.getProtobufClass().getMethod("parseFrom", InputStream.class);
        return new ResponseWrapper<T>((T) parseFromMethod.invoke(null, r.getResponseBodyAsStream()), r);
      };
    }
  },
  TEXT {
    @SuppressWarnings("unchecked")
    @Override
    <T> FailableFunction<Response, ResponseWrapper<T>, Exception> converterFunction(HttpClient context) {
      return r -> new ResponseWrapper<T>((T) r.getResponseBody(context.getCharset().name()), r);
    }
  },
  EMPTY {
    @SuppressWarnings("unused")
    @Override
    <T> FailableFunction<Response, ResponseWrapper<T>, Exception> converterFunction(HttpClient context) {
      return r -> new ResponseWrapper<T>(null, r);
    }
  };
  
  abstract <T> FailableFunction<Response, ResponseWrapper<T>, Exception> converterFunction(HttpClient context);
}
