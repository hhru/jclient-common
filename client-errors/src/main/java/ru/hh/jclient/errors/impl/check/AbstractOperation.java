package ru.hh.jclient.errors.impl.check;

import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.ResultWithStatus;
import ru.hh.jclient.errors.impl.HttpStatuses;
import ru.hh.jclient.errors.impl.OperationBase;
import ru.hh.jclient.errors.impl.PredicateWithStatus;

public abstract class AbstractOperation<T, O extends AbstractOperation<T, O>> extends OperationBase<O> {

  protected static final Logger logger = LoggerFactory.getLogger(AbstractOperation.class);

  protected final ResultWithStatus<T> wrapper;

  protected Optional<List<PredicateWithStatus<T>>> predicates = Optional.empty();
  protected Optional<T> defaultValue = Optional.empty();
  protected Optional<List<Integer>> proxiedStatusCodes;
  protected Optional<Function<Integer, Integer>> statusCodesConverter;

  protected AbstractOperation(
      ResultWithStatus<T> wrapper,
      Optional<Integer> errorStatusCode,
      Optional<List<Integer>> proxiedStatusCodes,
      Optional<Function<Integer, Integer>> statusCodesConverter,
      String errorMessage,
      List<PredicateWithStatus<T>> predicates) {
    super(errorStatusCode, errorMessage);
    this.wrapper = wrapper;
    this.proxiedStatusCodes = proxiedStatusCodes;
    this.statusCodesConverter = statusCodesConverter;
    this.errorStatusCode = getStatusCodeIfAbsent(wrapper, errorStatusCode, proxiedStatusCodes, statusCodesConverter);
    this.predicates = Optional.ofNullable(predicates);
  }

  protected AbstractOperation(
      ResultWithStatus<T> wrapper,
      Optional<Integer> errorStatusCode,
      Optional<List<Integer>> proxiedStatusCodes,
      Optional<Function<Integer, Integer>> statusCodesConverter,
      String errorMessage,
      List<PredicateWithStatus<T>> predicates,
      Optional<T> defaultValue) {
    this(wrapper, errorStatusCode, proxiedStatusCodes, statusCodesConverter, errorMessage, predicates);
    this.defaultValue = defaultValue;
  }

  // internal terminal operation implementations

  protected Optional<T> checkForAnyError() {
    Optional<T> optional = checkForStatusCodeError(); // status code check and unwrap
    optional = checkForPredicates(optional); // predicate check
    if (optional.isPresent()) {
      return optional;
    }
    return defaultOrThrow("result is empty");
  }

  protected Optional<T> checkForStatusCodeError() {
    if (wrapper.isSuccess()) {
      return wrapper.get();
    }
    return defaultOrThrow("status code is not OK");
  }

  protected Optional<T> checkForPredicates(Optional<T> response) {
    if (!response.isPresent() || !predicates.isPresent()) {
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
      errorStatusCode = predicate.getStatus().map(Optional::of).orElse(errorStatusCode);
      return true;
    }
    return false;
  }

  protected T checkForEmpty() {
    return wrapper.get().orElse(defaultOrThrow("result is empty").get());
  }

  protected Optional<T> defaultOrThrow(String cause) {
    if (useDefault()) {
      logger.warn("Default value is returned because error happened: {}. Description: {}", cause, errorResponseBuilder.getMessage());
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

    if (!currentStatusCode.isPresent()) {
      return errorStatusCode;
    }

    if (!errorStatusCode.isPresent()) {
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
