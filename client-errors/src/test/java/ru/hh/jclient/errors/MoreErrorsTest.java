package ru.hh.jclient.errors;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;
import javax.ws.rs.WebApplicationException;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import javax.ws.rs.ext.RuntimeDelegate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.hh.jclient.common.EmptyWithStatus;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.jersey.RuntimeDelegateStub;

public class MoreErrorsTest {

  @BeforeClass
  public static void setupStubs() {
    RuntimeDelegate.setInstance(new RuntimeDelegateStub());
  }

  // any error

  @Test
  public void testAnyErrorsWithOkStatus() {
    ResultWithStatus<String> result = new ResultWithStatus<>("zxc", OK.getStatusCode());
    String value = MoreErrors.check(result, "error").throwBadGateway().onAnyError();
    assertEquals(value, "zxc");
  }

  @Test(expected = WebApplicationException.class)
  public void testAnyErrorsWithNonOkStatus() {
    ResultWithStatus<String> result = new ResultWithStatus<>("zxc", INTERNAL_SERVER_ERROR.getStatusCode());
    MoreErrors.check(result, "error").throwBadGateway().onAnyError();
  }

  @Test(expected = WebApplicationException.class)
  public void testAnyErrorsWithPredicate() {
    Predicate<String> predicate = s -> s.equals("zxc"); // zxc value means failure
    ResultWithStatus<String> result = new ResultWithStatus<>("zxc", OK.getStatusCode());
    MoreErrors.check(result, "error").failIf(predicate).throwBadGateway().onAnyError();
  }

  @Test
  public void testAnyErrorsWithPredicateAndStatus() {
    ResultWithStatus<String> result = new ResultWithStatus<>("zxc", OK.getStatusCode());
    try {
      MoreErrors.check(result, "error").failIf(s -> s.equals("zxc"), NOT_FOUND).throwForbidden().onAnyError();
    }
    catch (WebApplicationException e) {
      assertEquals(e.getResponse().getStatus(), NOT_FOUND.getStatusCode());
    }
  }

  @Test(expected = WebApplicationException.class)
  public void testAnyErrorsWithEmpty() {
    ResultWithStatus<String> result = new ResultWithStatus<>(null, OK.getStatusCode());
    MoreErrors.check(result, "error").throwBadGateway().onAnyError();
  }

  // status code error

  @Test(expected = WebApplicationException.class)
  public void testStatusCodeErrorWithNonOkStatus() {
    ResultWithStatus<String> result = new ResultWithStatus<>("zxc", INTERNAL_SERVER_ERROR.getStatusCode());
    MoreErrors.check(result, "error").throwBadGateway().onStatusCodeError();
  }

  @Test
  public void testStatusCodeErrorWithAllOtherFailures() {
    ResultWithStatus<String> result;
    Optional<String> value;

    // predicate means nothing here
    Predicate<String> predicate = s -> s.equals("zxc"); // zxc value means failure
    result = new ResultWithStatus<>("zxc", OK.getStatusCode());
    value = MoreErrors.check(result, "error").failIf(predicate).throwBadGateway().onStatusCodeError();
    assertEquals(value.get(), "zxc");

    // empty means nothing here
    result = new ResultWithStatus<>(null, OK.getStatusCode());
    value = MoreErrors.check(result, "error").throwBadGateway().onStatusCodeError();
    assertFalse(value.isPresent());
  }

  // predicate error

  @Test(expected = WebApplicationException.class)
  public void testPredicateWithIncorrectValue() {
    Predicate<String> predicate = s -> s.equals("zxc"); // zxc value means failure
    ResultWithStatus<String> result = new ResultWithStatus<>("zxc", OK.getStatusCode());
    MoreErrors.check(result, "error").failIf(predicate).throwBadGateway().onPredicate();
  }

  @Test
  public void testPredicateWithCorrectAnyValue() {
    Predicate<String> predicate = s -> s.equals("zxc"); // zxc value means failure
    ResultWithStatus<String> result;
    Optional<String> value;

    // empty means nothing here
    result = new ResultWithStatus<>(null, OK.getStatusCode());
    value = MoreErrors.check(result, "error").failIf(predicate).throwBadGateway().onPredicate();
    assertFalse(value.isPresent());

    // status means nothing here
    result = new ResultWithStatus<>("asd", INTERNAL_SERVER_ERROR.getStatusCode());
    value = MoreErrors.check(result, "error").failIf(predicate).throwBadGateway().onPredicate();
    assertEquals(value.get(), "asd");
  }

  // default value

  @Test
  public void testDefaultValueOnSomeError() {
    ResultWithStatus<String> result;
    Optional<String> value;
    String realValue;

    // any error
    Predicate<String> predicate = "zxc"::equals; // zxc value means failure
    result = new ResultWithStatus<>(null, INTERNAL_SERVER_ERROR.getStatusCode());
    realValue = MoreErrors.check(result, "error").failIf(predicate).returnDefault("error").onAnyError();
    assertEquals(realValue, "error");

    // status code
    result = new ResultWithStatus<>(null, INTERNAL_SERVER_ERROR.getStatusCode());
    value = MoreErrors.check(result, "error").returnDefault("error").onStatusCodeError();
    assertEquals(value.get(), "error");

    // empty
    result = new ResultWithStatus<>(null, OK.getStatusCode());
    realValue = MoreErrors.check(result, "error").failIf(predicate).returnDefault("error").onEmpty();
    assertEquals(realValue, "error");

    // predicate
    result = new ResultWithStatus<>("zxc", OK.getStatusCode());
    value = MoreErrors.check(result, "error").failIf(predicate).returnDefault("error").onPredicate();
    assertEquals(value.get(), "error");
  }

  // empty value

  @Test
  public void testEmptyValueOnSomeError() {
    EmptyWithStatus result;
    Optional<Void> value;

    // status code
    result = new EmptyWithStatus(INTERNAL_SERVER_ERROR.getStatusCode());
    value = MoreErrors.check(result, "error").returnEmpty().onStatusCodeError();
    assertTrue(value.isEmpty());

    // predicate
    Predicate<Void> predicate = (v) -> true;
    result = new EmptyWithStatus(OK.getStatusCode());
    value = MoreErrors.check(result, "error").failIf(predicate).returnEmpty().onPredicate();
    assertTrue(value.isEmpty());
  }

  // special cases

  @Test
  public void testServiceUnavailableNotProxied() {
    ResultWithStatus<String> result = new ResultWithStatus<>(null, SERVICE_UNAVAILABLE.getStatusCode());
    try {
      MoreErrors.check(result, "error").proxyStatusCode().onAnyError();
    }
    catch (WebApplicationException e) {
      assertEquals(e.getResponse().getStatus(), HttpStatuses.BAD_GATEWAY);
    }
  }

  // error conversion

  @Test
  public void testExceptionConversion() {
    String result = "zxc";
    Throwable exception = new FileNotFoundException(); // descendant of IOException

    // return result if there is no exception
    String realResult = MoreErrors.convertException(result, null, "error").throwGatewayTimeout().on(FileNotFoundException.class);
    assertEquals(realResult, result);

    // throw WAE with HTTP GATEWAY TIMEOUT for expected (one of) error exact class
    try {
      MoreErrors.convertException(result, exception, "error").throwGatewayTimeout().on(FileNotFoundException.class, IllegalArgumentException.class);
    }
    catch (Throwable t) {
      ensureExceptionConverted(t);
    }

    // throw WAE with HTTP GATEWAY TIMEOUT for expected error superclass
    try {
      MoreErrors.convertException(result, exception, "error").throwGatewayTimeout().on(IOException.class);
    }
    catch (Throwable t) {
      ensureExceptionConverted(t);
    }

    // throw WAE with HTTP GATEWAY TIMEOUT for expected error superclass even if it is wrapped in CompletionException
    try {
      MoreErrors.convertException(result, new CompletionException(exception), "error").throwGatewayTimeout().on(IOException.class);
    }
    catch (Throwable t) {
      ensureExceptionConverted(t);
    }

    // throw original exception when specified exception class does not match
    try {
      Throwable otherException = new NullPointerException();
      MoreErrors.convertException(result, otherException, "error").throwGatewayTimeout().on(IOException.class, IllegalArgumentException.class);
    }
    catch (Throwable t) {
      assertEquals(t.getClass(), NullPointerException.class);
    }

    // throw unchanged webapplication exception even if we expect it
    try {
      WebApplicationException wae = new WebApplicationException(777);
      MoreErrors.convertException(result, new CompletionException(wae), "error").throwGatewayTimeout().on(WebApplicationException.class);
    }
    catch (Throwable t) {
      assertEquals(t.getClass(), WebApplicationException.class);
      WebApplicationException wae = (WebApplicationException) t;
      assertEquals(wae.getResponse().getStatus(), 777);
    }
  }

  private void ensureExceptionConverted(Throwable t) {
    assertTrue(t instanceof WebApplicationException);
    WebApplicationException wae = (WebApplicationException) t;
    assertEquals(wae.getResponse().getStatus(), HttpStatuses.GATEWAY_TIMEOUT);
  }

  // handle ignore

  @Test
  public void testIgnore() {
    ResultWithStatus<String> result = null;
    Optional<String> realValue;

    // exception
    realValue = MoreErrors.check(result, new NullPointerException(), "error").ignore().onAnyError();
    assertFalse(realValue.isPresent());

    // response error
    result = new ResultWithStatus<>(null, INTERNAL_SERVER_ERROR.getStatusCode());
    realValue = MoreErrors.check(result, (Throwable) null, "error").failIf("zxc"::equals).ignore().onAnyError();
    assertFalse(realValue.isPresent());

    // provided default value with exception
    realValue = MoreErrors.check(result, new NullPointerException(), "error").returnDefault("asd").onAnyError();
    assertTrue(realValue.isPresent());
    assertEquals(realValue.get(), "asd");

    // provided default value with response error
    result = new ResultWithStatus<>("zxc", INTERNAL_SERVER_ERROR.getStatusCode());
    realValue = MoreErrors.check(result, (Throwable) null, "error").returnDefault("asd").onAnyError();
    assertTrue(realValue.isPresent());
    assertEquals(realValue.get(), "asd");

    // success
    result = new ResultWithStatus<>("zxc", OK.getStatusCode());
    realValue = MoreErrors.check(result, (Throwable) null, "error").ignore().onAnyError();
    assertTrue(realValue.isPresent());
    assertEquals(realValue.get(), "zxc");

    // provided default value with response error
    result = new ResultWithStatus<>(null, INTERNAL_SERVER_ERROR.getStatusCode());
    realValue = MoreErrors.check(result, (Throwable) null, "error").returnDefault("asd").onStatusCodeError();
    assertTrue(realValue.isPresent());
    assertEquals(realValue.get(), "asd");
  }

  @Test
  public void testIgnoreForEmptyValue() {
    EmptyWithStatus result = null;
    Optional<Void> realValue;

    // exception
    realValue = MoreErrors.check(result, new NullPointerException(), "error").ignore().onStatusCodeError();
    assertFalse(realValue.isPresent());

    // response error
    result = new EmptyWithStatus(INTERNAL_SERVER_ERROR.getStatusCode());
    realValue = MoreErrors.check(result, (Throwable) null, "error").ignore().onStatusCodeError();
    assertFalse(realValue.isPresent());

    // success
    result = new EmptyWithStatus(NO_CONTENT.getStatusCode());
    realValue = MoreErrors.check(result, (Throwable) null, "error").ignore().onStatusCodeError();
    assertFalse(realValue.isPresent());
  }

  // proxying and converting status

  @Test
  public void testProxyStatus() {
    ResultWithStatus<String> result = new ResultWithStatus<>(null, BAD_REQUEST.getStatusCode());
    try {
      MoreErrors.check(result, "error").proxyOnly(BAD_REQUEST, NOT_FOUND).throwForbidden().onAnyError();
    }
    catch (WebApplicationException e) {
      assertEquals(e.getResponse().getStatus(), BAD_REQUEST.getStatusCode());
    }
  }

  @Test
  public void testConvertAndProxyStatus() {
    ResultWithStatus<String> result = new ResultWithStatus<>(null, BAD_REQUEST.getStatusCode());
    try {
      MoreErrors.check(result, "error").convertAndProxy(BAD_REQUEST, CONFLICT).throwForbidden().onAnyError();
    }
    catch (WebApplicationException e) {
      assertEquals(e.getResponse().getStatus(), CONFLICT.getStatusCode());
    }
  }

  // parameterized error messages

  @Test
  public void testParameterizedErrorMessage() {
    ResultWithStatus<String> result = new ResultWithStatus<>(null, BAD_REQUEST.getStatusCode());
    try {
      MoreErrors.check(result, "error %s", "custom").throwForbidden().onAnyError();
    }
    catch (WebApplicationException e) {
      assertEquals("error custom - status code 400 is not OK", e.getMessage());
    }
    try {
      MoreErrors.check(result, "error %s").throwForbidden().onAnyError();
    }
    catch (WebApplicationException e) {
      assertEquals("error %s - status code 400 is not OK", e.getMessage());
    }
  }

}
