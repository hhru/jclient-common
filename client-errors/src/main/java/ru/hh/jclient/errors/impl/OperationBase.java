package ru.hh.jclient.errors.impl;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import ru.hh.jclient.errors.ErrorsFactory;

public abstract class OperationBase<OB extends OperationBase<OB>> {

  protected Optional<Integer> errorStatusCode;
  protected final ErrorResponseBuilder errorResponseBuilder;
  protected Supplier<BiFunction<String, Integer, Object>> errorEntityCreatorSupplier;

  public OperationBase(Optional<Integer> errorStatusCode, Supplier<String> errorMessage) {
    this.errorStatusCode = errorStatusCode;
    this.errorResponseBuilder = new ErrorResponseBuilder(errorMessage);
  }

  protected WebApplicationException toException(String cause) {
    if (errorEntityCreatorSupplier != null) {
      this.errorResponseBuilder.setEntityCreator(errorEntityCreatorSupplier.get());
    }
    return errorResponseBuilder.setStatus(errorStatusCode.get()).appendToMessage("- " + cause).toWebApplicationException();
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
