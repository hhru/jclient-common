package ru.hh.jclient.errors.impl.check;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import javax.ws.rs.core.Response.Status;
import ru.hh.jclient.errors.impl.ExceptionBuilder;
import ru.hh.jclient.errors.impl.OperationSelectorBase;
import ru.hh.jclient.errors.impl.PredicateWithStatus;
import ru.hh.jclient.errors.impl.WebApplicationExceptionBuilder;

public abstract class AbstractOperationSelector<T, D extends AbstractOperationSelector<T, D>> extends OperationSelectorBase {

  public AbstractOperationSelector(String errorMessage, Object... params) {
    super(errorMessage, params);
  }

  protected List<PredicateWithStatus<T>> predicates = null;
  protected Set<Integer> allowedStatuses = new HashSet<>();
  protected ExceptionBuilder<?, ?> exceptionBuilder = new WebApplicationExceptionBuilder();

  private List<PredicateWithStatus<T>> predicates() {
    if (predicates == null) {
      predicates = new ArrayList<>();
    }
    return predicates;
  }

  /**
   * <p>
   * Specifies predicate that will be checked against the result. If predicate returns 'true', that means result is INCORRECT. If called multiple
   * times, resulting predicate will be built with {@link Predicate#or(Predicate)}.
   * </p>
   * <code>
   * .thenApply(rws -> check(rws, "failed to get vacancy")<b>.failIf(vac -> vac.isArchived())</b>.throwForbidden().onAnyError();
   * </code>
   * <p>
   * This will throw WAE with 403 status on any errors including case of archived vacancy.
   * </p>
   *
   * @param predicate
   *          predicate to add
   */
  public D failIf(Predicate<T> predicate) {
    return failIf(predicate, (Integer) null);
  }

  /**
   * <p>
   * Specifies predicate that will be checked against the result. If predicate returns 'true', that means result is INCORRECT. If called multiple
   * times, resulting predicate will be built with {@link Predicate#or(Predicate)}.
   * </p>
   * <code>
   * .thenApply(rws -> check(rws, "failed to get vacancy")<b>.failIf(vac -> vac.isArchived(), 404)</b>.throwForbidden().onAnyError();
   * </code>
   * <p>
   * This will throw WAE with 404 if predicate returns 'true', or 403 on any other errors.
   * </p>
   *
   * @param predicate
   *          predicate to add
   * @param status
   *          response status code to set in case predicate matched
   *
   */
  public D failIf(Predicate<T> predicate, Integer status) {
    predicates().add(new PredicateWithStatus<>(predicate, status));
    return getSelf();
  }

  /**
   * <p>
   * Specifies predicate that will be checked against the result. If predicate returns 'true', that means result is INCORRECT. If called multiple
   * times, resulting predicate will be built with {@link Predicate#or(Predicate)}.
   * </p>
   * <code>
   * .thenApply(rws -> check(rws, "failed to get vacancy")<b>.failIf(vac -> vac.isArchived(), Status.NOT_FOUND)</b>.throwForbidden().onAnyError();
   * </code>
   * <p>
   * This will throw WAE with 404 if predicate returns 'true', or 403 on any other errors.
   * </p>
   *
   * @param predicate
   *          predicate to add
   * @param status
   *          response status code to set in case predicate matched
   *
   */
  public D failIf(Predicate<T> predicate, Status status) {
    return failIf(predicate, status.getStatusCode());
  }

  public D allow(Status status) {
    allowedStatuses.add(status.getStatusCode());
    return getSelf();
  }

  public D allow(int code) {
    allowedStatuses.add(code);
    return getSelf();
  }

  public D exceptionBuilder(ExceptionBuilder<?, ?> exceptionBuilder) {
    this.exceptionBuilder = exceptionBuilder;
    return getSelf();
  }

  @SuppressWarnings("unchecked")
  protected D getSelf() {
    return (D) this;
  }
}
