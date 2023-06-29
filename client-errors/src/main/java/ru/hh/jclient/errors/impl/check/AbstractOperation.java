package ru.hh.jclient.errors.impl.check;

import jakarta.annotation.Nullable;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
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

  protected List<PredicateWithStatus<T>> predicates;
  protected Set<Integer> allowStatuses;
  @Nullable
  protected T defaultValue;
  private boolean useDefault = false;
  protected List<Integer> proxiedStatusCodes;

  protected AbstractOperation(
      ResultWithStatus<T> wrapper,
      @Nullable Integer errorStatusCode,
      List<Integer> proxiedStatusCodes,
      @Nullable Function<Integer, Integer> statusCodesConverter,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<T>> predicates,
      Set<Integer> allowStatuses,
      ExceptionBuilder<?, ?> exceptionBuilder) {
    super(
        getStatusCodeIfAbsent(wrapper, errorStatusCode, proxiedStatusCodes, statusCodesConverter).orElse(null),
        wrapper == null ? null : wrapper.getStatusCode(),
        errorMessage,
        exceptionBuilder
    );
    this.wrapper = wrapper;
    this.proxiedStatusCodes = proxiedStatusCodes;
    this.predicates = predicates;
    this.allowStatuses = allowStatuses;
  }

  protected AbstractOperation(
      ResultWithStatus<T> wrapper,
      @Nullable Integer errorStatusCode,
      List<Integer> proxiedStatusCodes,
      @Nullable Function<Integer, Integer> statusCodesConverter,
      Supplier<String> errorMessage,
      List<PredicateWithStatus<T>> predicates,
      @Nullable T defaultValue,
      Set<Integer> allowStatuses,
      ExceptionBuilder<?, ?> exceptionBuilder) {
    this(wrapper, errorStatusCode, proxiedStatusCodes, statusCodesConverter, errorMessage, predicates, allowStatuses, exceptionBuilder);
    this.defaultValue = defaultValue;
    this.useDefault = true;
  }

  // internal terminal operation implementations

  protected Optional<T> checkForAnyError() {
    Optional<T> optional = checkForStatusCodeError(); // status code check and unwrap
    optional = checkForPredicates(optional.orElse(null)); // predicate check
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
      return Optional.ofNullable(defaultValue).or(wrapper::get);
    }
    return defaultOrThrow("status code " + wrapper.getStatusCode() + " is not OK");
  }

  protected Optional<T> checkForPredicates(@Nullable T response) {
    if (response == null) {
      return Optional.empty();
    }

    boolean matched = predicates.stream().map(p -> testPredicate(p, response)).anyMatch(b -> b.equals(Boolean.TRUE));

    if (!matched) {
      return Optional.of(response);
    }
    return defaultOrThrow("predicate failed");
  }

  private boolean testPredicate(PredicateWithStatus<T> predicate, T response) {
    if (predicate.getPredicate().test(response)) {
      errorStatusCode = predicate.getStatus().orElse(errorStatusCode);
      return true;
    }
    return false;
  }

  protected Optional<T> checkForEmpty() {
    return wrapper.get().or(() -> defaultOrThrow("result is empty"));
  }

  protected Optional<T> defaultOrThrow(String cause) {
    if (useDefault()) {
      logger.warn("Default value is returned because error happened: {}. Description: {}", cause, exceptionBuilder.getMessage());
      return Optional.ofNullable(defaultValue);
    }
    throw toException(cause);
  }

  protected boolean useDefault() {
    return useDefault;
  }

  protected static Optional<Integer> getStatusCodeIfAbsent(
      ResultWithStatus<?> wrapper,
      @Nullable Integer errorStatusCode,
      List<Integer> proxiedStatusCodes,
      @Nullable Function<Integer, Integer> statusCodesConverter
  ) {
    if (wrapper == null) {
      return Optional.ofNullable(errorStatusCode);
    }

    int currentStatusCode = wrapper.getStatusCode();
    // replace 503 with 502 to avoid retries at intbal
    if (currentStatusCode == SERVICE_UNAVAILABLE.getStatusCode()) {
      currentStatusCode = HttpStatuses.BAD_GATEWAY;
    }

    if (errorStatusCode == null) {
      errorStatusCode = currentStatusCode;
    }

    // if current code can be proxied set it as error
    if (proxiedStatusCodes.contains(currentStatusCode)) {
      errorStatusCode = currentStatusCode;
    }

    // if converter changes current status code set result as error
    Integer converted = statusCodesConverter != null ? statusCodesConverter.apply(currentStatusCode) : currentStatusCode;
    if (!converted.equals(currentStatusCode)) {
      errorStatusCode = converted;
    }
    return Optional.of(errorStatusCode);
  }
}
