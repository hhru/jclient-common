package ru.hh.jclient.common;

public class EmptyWithResponse extends ResultWithResponse<Void> {

  public EmptyWithResponse(Response response) {
    super(null, response);
  }

  @Override
  public EmptyWithStatus hideResponse() {
    return new EmptyWithStatus(response.getStatusCode());
  }
}
