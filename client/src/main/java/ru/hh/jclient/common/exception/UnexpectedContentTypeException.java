package ru.hh.jclient.common.exception;

import java.util.Collection;
import java.util.stream.Collectors;
import com.google.common.net.MediaType;
import com.ning.http.client.Response;

public class UnexpectedContentTypeException extends ClientResponseException {

  private MediaType actual;
  private Collection<MediaType> expected;

  public UnexpectedContentTypeException(Response response, MediaType actual, Collection<MediaType> expected) {
    super(response, String.format(
      "Unexpected content type: %s, should be %s", actual.toString(), expected.stream().map(Object::toString).collect(Collectors.joining(","))));
    this.actual = actual;
    this.expected = expected;
  }

  public MediaType getActual() {
    return actual;
  }

  public Collection<MediaType> getExpected() {
    return expected;
  }
}
