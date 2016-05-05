package ru.hh.jclient.errors.impl;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

public class ErrorResponseBuilder {

  private Supplier<String> message;
  private int status;
  private Optional<BiFunction<String, Integer, Object>> entityCreator = Optional.empty();
  private Optional<Throwable> cause = Optional.empty();

  /**
   * Construct builder.
   *
   * @param message
   *          used for setting {@link Throwable#getMessage()} as well as passing it for entity creator. Can be null.
   */
  public ErrorResponseBuilder(Supplier<String> message) {
    this.message = message;
  }

  public String getMessage() {
    return message.get();
  }

  /**
   * Set function that is able to produce entity for WAE's response. Function accepts message and status code and produces any object that can be
   * serialized to framework's format of choice.
   */
  public ErrorResponseBuilder setEntityCreator(BiFunction<String, Integer, Object> entityCreator) {
    this.entityCreator = Optional.of(entityCreator);
    return this;
  }

  public ErrorResponseBuilder setStatus(int status) {
    this.status = status;
    return this;
  }

  public ErrorResponseBuilder appendToMessage(String addition) {
    if (this.message == null) {
      this.message = () -> addition;
    }
    else {
      // if we're appending something then most likely full error message will be needed anyway so we can unwrap current supplier even if it is
      // expensive
      // also, calling this.message.get() in new supplier will lead to stack overflow
      String currentMessage = this.message.get();
      this.message = () -> currentMessage + " " + addition;
    }
    return this;
  }

  public ErrorResponseBuilder setCause(Throwable cause) {
    this.cause = Optional.ofNullable(cause);
    return this;
  }

  public WebApplicationException toWebApplicationException() {
    ResponseBuilder builder = Response.status(status);
    String message = this.message == null ? null : this.message.get();
    entityCreator.ifPresent(ec -> builder.entity(ec.apply(message, status)));
    Response response = builder.build();
    CustomWebApplicationException exception = cause
        .map(c -> new CustomWebApplicationException(c, response))
        .orElseGet(() -> new CustomWebApplicationException(response));
    exception.setMessage(message);
    return exception;
  }

}