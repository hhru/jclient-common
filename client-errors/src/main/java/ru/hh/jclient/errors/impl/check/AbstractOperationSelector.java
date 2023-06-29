package ru.hh.jclient.errors.impl.check;

import jakarta.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.errors.impl.ExceptionBuilder;
import ru.hh.jclient.errors.impl.OperationSelectorBase;
import ru.hh.jclient.errors.impl.PredicateWithStatus;
import ru.hh.jclient.errors.impl.WebApplicationExceptionBuilder;

public abstract class AbstractOperationSelector<T, D extends AbstractOperationSelector<T, D>> extends OperationSelectorBase {

  public AbstractOperationSelector(String errorMessage, Object... params) {
    super(errorMessage, params);
    this.exceptionBuilder = new WebApplicationExceptionBuilder();
  }

  protected List<PredicateWithStatus<T>> predicates = new ArrayList<>();
  protected final Set<Integer> allowedStatuses = new HashSet<>();
  protected ExceptionBuilder<?, ?> exceptionBuilder;

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
    predicates.add(new PredicateWithStatus<>(predicate, status));
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

  /**
   * <p>
   * Specifies status that will be allowed in range of fail statuses. For this status checkStatusCodeError will not throw Exception,
   * and empty result will be returned. If called multiple times, all statuses will be allowed.
   * </p>
   * <code>
   * .thenApply(rws -> check(rws, "failed to get vacancy")<b>.allow(Status.NOT_FOUND)</b>.throwForbidden().onAnyError();
   * </code>
   * <p>
   * This will throw WAE with 403 for any failure status except 404.
   * </p>
   *
   * @param status
   *          allowed response status
   *
   */
  public D allow(Status status) {
    return allow(status.getStatusCode());
  }

  public D allow(Status... statuses) {
    return allow(Stream.of(statuses).map(Status::getStatusCode).toArray(Integer[]::new));
  }

  public D allow(int code) {
    if (HttpClient.OK_RANGE.contains(code)) {
      throw new IllegalArgumentException("allowed status can't be lower than 400");
    }
    allowedStatuses.add(code);
    return getSelf();
  }

  public D allow(Integer... code) {
    if (Stream.of(code).anyMatch(HttpClient.OK_RANGE::contains)) {
      throw new IllegalArgumentException("allowed status can't be lower than 400");
    }
    allowedStatuses.addAll(Arrays.asList(code));
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
