package ru.hh.jclient.common;

import ru.hh.jclient.common.util.MoreFunctionalInterfaces.FailableFunction;

import com.ning.http.client.Response;

public abstract class AbstractConverter<T> {

  protected abstract FailableFunction<Response, ResponseWrapper<T>, Exception> converterFunction();

}
