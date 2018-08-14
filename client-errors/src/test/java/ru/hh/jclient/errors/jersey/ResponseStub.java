package ru.hh.jclient.errors.jersey;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

final class ResponseStub extends Response {

  private final Object entity;
  private final StatusType status;

  ResponseStub(int statusCode, Object entity) {
    this.entity = entity;

    status = new StatusType() {
      @Override
      public int getStatusCode() {
        return statusCode;
      }

      @Override
      public Status.Family getFamily() {
        return null;
      }

      @Override
      public String getReasonPhrase() {
        return null;
      }
    };
  }

  @Override
  public int getStatus() {
    return status.getStatusCode();
  }

  @Override
  public Object getEntity() {
    return entity;
  }

  @Override
  public MultivaluedMap<String, Object> getMetadata() {
    throw new UnsupportedOperationException();
  }
}
