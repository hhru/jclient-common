package ru.hh.jclient.errors;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public abstract class AbstractErrorHandlerBase<HB extends AbstractErrorHandlerBase<HB>> {

  protected Optional<Integer> errorStatusCode;
  protected final ErrorResponseBuilder errorResponseBuilder;
  protected Supplier<BiFunction<String, Integer, Object>> errorEntityCreatorSupplier;

  public AbstractErrorHandlerBase(Optional<Integer> errorStatusCode, String errorMessage) {
    this.errorStatusCode = errorStatusCode;
    this.errorResponseBuilder = new ErrorResponseBuilder(errorMessage);
  }

  protected WebApplicationException toException(String cause) {
    if (errorEntityCreatorSupplier != null) {
      this.errorResponseBuilder.setEntityCreator(errorEntityCreatorSupplier.get());
    }
    return errorResponseBuilder.setStatus(errorStatusCode.get()).appendToMessage(cause).toException();
  }

  /**
   * Specifies how to create an entity that will be used in response in case of error. Example:
   *
   * <pre>
   * MoreErrors.check(rws, "Request failed").THROW_BAD_REQUEST().as((message, status) -> new XmlError(message, status)).onAnyError()
   * </pre>
   *
   * In this example when error is detected, XmlError entity with "Request failed" message and {@link Response.Status#BAD_REQUEST} status code will be
   * created and set to {@link Response} wrapped in {@link WebApplicationException}.
   *
   * @param errorEntityCreator
   *          first argument is error message, second argument is response status code
   */
  public HB as(BiFunction<String, Integer, Object> errorEntityCreator) {
    this.errorEntityCreatorSupplier = () -> errorEntityCreator;
    return getDerivedClass().cast(this);
  }

  /**
   * Same as {@link #as(BiFunction)}} but accepts supplier as opposed to existing bi-function.
   */
  public HB as(Supplier<BiFunction<String, Integer, Object>> errorEntityCreatorSupplier) {
    this.errorEntityCreatorSupplier = errorEntityCreatorSupplier;
    return getDerivedClass().cast(this);
  }

  protected abstract Class<HB> getDerivedClass();
}
