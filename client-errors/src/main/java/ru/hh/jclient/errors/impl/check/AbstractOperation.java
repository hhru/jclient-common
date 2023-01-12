package ru.hh.jclient.errors.impl.check;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpStatuses;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.impl.ExceptionBuilder;
import ru.hh.jclient.errors.impl.OperationBase;
import ru.hh.jclient.errors.impl.PredicateWithStatus;

public abstract class AbstractOperation<T, O extends AbstractOperation<T, O>> extends OperationBase<O> {

  protected static final Logger logger = LoggerFactory.getLogger(AbstractOperation.class);

  protected final ResultWithStatus<T> wrapper;

  protected Optional<List<PredicateWithStatus<T>>> predicates = Optional.empty();
  protected Set<Integer> allowStatuses = Set.of();
  protected Optional<T> defaultValue = Optional.empty();
  protected Optional<List<Integer>> proxiedStatusCodes;

  protected AbstractOperation(
      ResultWithStatus<T> wrapper,
      Optional<Integer> errorStatusCode,
      Optional<List<Integer>> proxiedStatusCodes,
      Optional<Function<Integer, Integer>> statusCodesConverter,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<T>> predicates,
      Set<Integer> allowStatuses,
      ExceptionBuilder<?, ?> exceptionBuilder) {
    super(getStatusCodeIfAbsent(wrapper, errorStatusCode, proxiedStatusCodes, statusCodesConverter),
        wrapper == null ? null : wrapper.getStatusCode(),
        errorMessage,
        exceptionBuilder);
    this.wrapper = wrapper;
    this.proxiedStatusCodes = proxiedStatusCodes;
    this.predicates = Optional.ofNullable(predicates);
    this.allowStatuses = allowStatuses;
  }

  protected AbstractOperation(
      ResultWithStatus<T> wrapper,
      Optional<Integer> errorStatusCode,
      Optional<List<Integer>> proxiedStatusCodes,
      Optional<Function<Integer, Integer>> statusCodesConverter,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<T>> predicates,
      Optional<T> defaultValue,
      Set<Integer> allowStatuses,
      ExceptionBuilder<?, ?> exceptionBuilder) {
    this(wrapper, errorStatusCode, proxiedStatusCodes, statusCodesConverter, errorMessage, predicates, allowStatuses, exceptionBuilder);
    this.defaultValue = defaultValue;
  }

  // internal terminal operation implementations

  protected Optional<T> checkForAnyError() {
    Optional<T> optional = checkForStatusCodeError(); // status code check and unwrap
    optional = checkForPredicates(optional); // predicate check
    if (optional.isPresent() || allowStatuses.contains(wrapper.getStatusCode())) {
      return optional;
    }
    return defaultOrThrow("result is empty");
  }

  protected Optional<T> checkForStatusCodeError() {
    if (wrapper.isSuccess()) {
      return wrapper.get();
    }
    if (allowStatuses.contains(wrapper.getStatusCode())) {
      return defaultValue.or(wrapper::get);
    }
    return defaultOrThrow("status code " + wrapper.getStatusCode() + " is not OK");
  }

  protected Optional<T> checkForPredicates(Optional<T> response) {
    if (response.isEmpty() || predicates.isEmpty()) {
      return response;
    }
    T responseUnwrapped = response.get();
    boolean matched = predicates.get().stream().map(p -> testPredicate(p, responseUnwrapped)).anyMatch(b -> b.equals(Boolean.TRUE));

    if (!matched) {
      return response;
    }
    return defaultOrThrow("predicate failed");
  }

  private boolean testPredicate(PredicateWithStatus<T> predicate, T response) {
    if (predicate.getPredicate().test(response)) {
      errorStatusCode = predicate.getStatus().or(() -> errorStatusCode);
      return true;
    }
    return false;
  }

  protected T checkForEmpty() {
    return wrapper.get().orElseGet(defaultOrThrow("result is empty")::get);
  }

  protected Optional<T> defaultOrThrow(String cause) {
    if (useDefault()) {
      logger.warn("Default value is returned because error happened: {}. Description: {}", cause, exceptionBuilder.getMessage());
      return defaultValue;
    }
    throw toException(cause);
  }

  protected boolean useDefault() {
    return defaultValue.isPresent();
  }

  protected static Optional<Integer> getStatusCodeIfAbsent(
      ResultWithStatus<?> wrapper,
      Optional<Integer> errorStatusCode,
      Optional<List<Integer>> proxiedStatusCodes,
      Optional<Function<Integer, Integer>> statusCodesConverter) {
    if (wrapper == null) {
      return errorStatusCode;
    }
    Optional<Integer> currentStatusCode = Optional
        .of(wrapper.getStatusCode())
        // replace 503 with 502 to avoid retries at intbal
        .map(s -> s == SERVICE_UNAVAILABLE.getStatusCode() ? HttpStatuses.BAD_GATEWAY : s);

    if (errorStatusCode.isEmpty()) {
      errorStatusCode = currentStatusCode;
    }

    // if current code can be proxied set it as error
    if (proxiedStatusCodes.map(codes -> codes.contains(currentStatusCode.get())).orElse(false)) {
      errorStatusCode = currentStatusCode;
    }

    // if converter changes current status code set result as error
    Integer converted = statusCodesConverter.map(f -> f.apply(currentStatusCode.get())).orElseGet(currentStatusCode::get);
    if (!converted.equals(currentStatusCode.get())) {
      errorStatusCode = Optional.of(converted);
    }
    return errorStatusCode;
  }
}
