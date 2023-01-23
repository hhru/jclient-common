package ru.hh.jclient.errors.impl;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

public class WebApplicationExceptionBuilder extends ExceptionBuilder<WebApplicationException, WebApplicationExceptionBuilder> {

  @Override
  public WebApplicationException toException() {
    ResponseBuilder builder = Response.status(status);
    String message = this.message == null ? null : this.message.toString();
    if (entityCreator != null) {
      builder.entity(entityCreator.apply(message, status));
    }
    Response response = builder.build();
    return new WebApplicationException(message, cause, response);
  }
}
