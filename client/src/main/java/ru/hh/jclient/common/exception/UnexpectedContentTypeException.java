package ru.hh.jclient.common.exception;

import java.util.Collection;
import java.util.stream.Collectors;
import ru.hh.jclient.common.Response;

public class UnexpectedContentTypeException extends ClientResponseException {

  private String actual;
  private Collection<String> expected;

  public UnexpectedContentTypeException(Response response, String actual, Collection<String> expected) {
    super(
        response,
        String.format(
            "Unexpected content type: %s, should be %s",
            actual,
            expected.stream().map(Object::toString).collect(Collectors.joining(","))
        )
    );
    this.actual = actual;
    this.expected = expected;
  }

  public String getActual() {
    return actual;
  }

  public Collection<String> getExpected() {
    return expected;
  }
}
