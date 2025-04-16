package ru.hh.jclient.errors.impl;

import jakarta.annotation.Nullable;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import ru.hh.jclient.errors.ErrorsFactory;

public abstract class OperationBase<OB extends OperationBase<OB>> {

  @Nullable
  protected Integer errorStatusCode;
  private final Integer originalStatusCode;
  protected ExceptionBuilder<?, ?> exceptionBuilder;
  protected Supplier<BiFunction<String, Integer, Object>> errorEntityCreatorSupplier;

  public OperationBase(@Nullable Integer errorStatusCode, Integer originalStatusCode, Supplier<String> errorMessage) {
    this(errorStatusCode, originalStatusCode, errorMessage, new WebApplicationExceptionBuilder());
  }

  public OperationBase(
      @Nullable Integer errorStatusCode,
      Integer originalStatusCode,
      Supplier<String> errorMessage,
      ExceptionBuilder<?, ?> exceptionBuilder) {
    this.errorStatusCode = errorStatusCode;
    this.originalStatusCode = originalStatusCode;
    this.exceptionBuilder = exceptionBuilder.appendToMessage(errorMessage.get());
  }

  protected RuntimeException toException(String cause) {
    if (errorEntityCreatorSupplier != null) {
      this.exceptionBuilder.setEntityCreator(errorEntityCreatorSupplier.get());
    }
    return exceptionBuilder.setOriginalStatus(originalStatusCode).setStatus(errorStatusCode).appendToMessage("- " + cause).toException();
  }

  /**
   * Specifies how to create an entity that will be used in response in case of error. Example:
   *
   * <pre>
   * MoreErrors.check(rws, "Request failed").throwBadRequest().as((message, status) -> new XmlError(message, status)).onAnyError()
   * </pre>
   *
   * In this example when error is detected, XmlError entity with "Request failed" message and {@link Response.Status#BAD_REQUEST} status code will be
   * created and set to {@link Response} wrapped in {@link WebApplicationException}.
   *
   * @param errorEntityCreator
   *          first argument is error message, second argument is response status code
   */
  public OB as(BiFunction<String, Integer, Object> errorEntityCreator) {
    this.errorEntityCreatorSupplier = () -> errorEntityCreator;
    return getSelf();
  }

  /**
   * Same as {@link #as(BiFunction)}} but accepts supplier as opposed to existing bi-function.
   */
  public OB as(Supplier<BiFunction<String, Integer, Object>> errorEntityCreatorSupplier) {
    this.errorEntityCreatorSupplier = errorEntityCreatorSupplier;
    return getSelf();
  }

  /**
   * Sets error entity created using {@link ErrorsFactory#error(Object)}.
   *
   * @param key
   *          error key
   */
  public OB asError(Object key) {
    return as(ErrorsFactory.error(key));
  }

  @SuppressWarnings("unchecked")
  protected OB getSelf() {
    return (OB) this;
  }
}
