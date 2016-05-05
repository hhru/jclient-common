package ru.hh.jclient.errors;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import java.util.function.BiFunction;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import ru.hh.errors.common.AbstractErrors;
import ru.hh.errors.common.Errors;
import ru.hh.jclient.errors.impl.ErrorResponseBuilder;
import ru.hh.jclient.errors.impl.OperationBase;

public class ErrorsFactory {

  // avoid instantiation
  private ErrorsFactory() {
  }

  /**
   * Returns function to use with {@link OperationBase#as(BiFunction)}. Function will produce {@link Errors} object with specified error
   * key.
   *
   * @param key
   *          error key
   */
  public static BiFunction<String, Integer, Object> error(Object key) {
    return (s, i) -> new Errors(i, key.toString(), s);
  }

  /**
   * Returns {@link WebApplicationException} with {@link Errors} as response body.
   *
   * @param code
   *          status code to set to error response
   * @param errorKey
   *          error key
   * @param description
   *          error description
   */
  public static WebApplicationException error(int code, Object errorKey, String description, Throwable cause) {
    return new ErrorResponseBuilder(() -> description).setStatus(code).setEntityCreator(error(errorKey)).setCause(cause).toWebApplicationException();
  }

  /**
   * Returns {@link WebApplicationException} with provided error container as response body.
   *
   * @param errors
   *          errors container
   */
  public static WebApplicationException error(AbstractErrors<?> errors) {
    return new ErrorResponseBuilder(null).setStatus(errors.getCode()).setEntityCreator((s, i) -> errors).toWebApplicationException();
  }

  /**
   * Returns {@link WebApplicationException} with {@link Errors} as response body.
   *
   * @param status
   *          status code to set to error response
   * @param errorKey
   *          error key
   * @param description
   *          error description
   */
  public static WebApplicationException error(Status status, Object errorKey, String description, Throwable cause) {
    return error(status.getStatusCode(), errorKey, description, cause);
  }

  public static WebApplicationException errorBadRequest(Object errorKey, String description) {
    return error(BAD_REQUEST, errorKey, description, null);
  }

  public static <T> T throwBadRequest(Object errorKey, String description) {
    throw errorBadRequest(errorKey, description);
  }

  public static WebApplicationException errorConflict(Object errorKey, String description) {
    return error(CONFLICT, errorKey, description, null);
  }

  public static <T> T throwConflict(Object errorKey, String description) {
    throw errorConflict(errorKey, description);
  }

  public static WebApplicationException errorNotFound(Object errorKey, String description) {
    return error(NOT_FOUND, errorKey, description, null);
  }

  public static <T> T throwNotFound(Object errorKey, String description) {
    throw errorNotFound(errorKey, description);
  }

  public static WebApplicationException errorForbidden(Object errorKey, String description) {
    return error(FORBIDDEN, errorKey, description, null);
  }

  public static <T> T throwForbidden(Object errorKey, String description) {
    throw errorForbidden(errorKey, description);
  }
}
