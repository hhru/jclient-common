package ru.hh.jclient.errors.impl;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

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
