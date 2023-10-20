package ru.hh.jclient.common;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Supplier;
import ru.hh.jclient.common.errors.ErrorsFactory;
import ru.hh.jclient.common.errors.ExceptionBuilder;

public class Examples {

  private final TestClient client = new TestClient(null, "url");

  public void ignoreErrors() {
    Optional<TestDto> dataMaybe = client.getData(10).uncheckedResult();
  }

  public void resultWithStatus() {
    TestDto data = client.getData(10)
        .checkResult("Failed to get test data").throwBadGateway().onAnyError();
  }

  public void resultOrErrorWithStatus() {

    // Handle error dto and response

    TestDto data = client.getDataOrDie(10)
        .checkErrorResult("Failed to get test data")
        .throwBadRequest()
        .as(err -> ErrorsFactory.error(err.errorKey()))
        .onAnyError()
        .checkResult("Failed to get test data")
        .throwBadGateway()
        .onAnyError();

    // Ignore error dto and handle response only

    TestDto data1 = client.getDataOrDie(10)
        .checkResult("Failed to get test data").throwBadGateway().onAnyError();
  }

  public void emptyOrErrorWithStatus() {

    // Handle error dto and response

    client.deleteData(10)
        .checkEmptyErrorResult("Failed to delete data for id %d because of SQL error", 10)
        .failIf(err -> err.errorMessage().contains("SQL"))
        .throwInternalServerError()
        .onAnyError()
        .checkEmptyResult("Failed to delete data for id %d, ignoring", 10)
        .returnEmpty()
        .onStatusCodeError();

    // Ignore error dto and handle response only

    client.deleteData(10)
        .checkEmptyResult("Failed to delete data for id %d, ignoring", 10)
        .returnEmpty()
        .onStatusCodeError();
  }

  public void reconfigureClient() {
    TestDto data = client.configure()
        .header("X-Real-IP", "127.0.0.1")
        .queryParam("backOfficeAllowed", "true")
        // returns new instance with overrides applied, original instance is unmodified
        .configured()
        .getData(10)
        .checkResult("Failed to get data")
        .exceptionBuilder(new RuntimeExceptionBuilder())
        .throwInternalServerError()
        .onAnyError();
  }

  public void sequentialRequests() {
    TestDto data = client.getData(11)
        .checkResult("Failed to get data for 11").throwNotFound().onAnyError();
    client.deleteData(data.id())
        .checkEmptyResult("Failed to delete data for 11")
        .throwInternalServerError().onStatusCodeError();
    client.reportDeleted(data.id())
        .checkEmptyResult("Failed to report data for 11")
        .returnEmpty().onStatusCodeError();
  }

  public void parallelRequests() throws InterruptedException, ExecutionException {

    try (var getting = new StructuredTaskScope.ShutdownOnSuccess<TestDto>()) { // any will finish
      Supplier<TestDto> eleven = getting.fork(() -> client.getData(11).checkResult("Failed to get data for 11").throwNotFound().onAnyError());
      Supplier<TestDto> twelve = getting.fork(() -> client.getData(12).checkResult("Failed to get data for 12").throwNotFound().onAnyError());

      TestDto result = getting.join().result();

      try (var mutating = new StructuredTaskScope.ShutdownOnFailure()) { // all will finish
        mutating.fork(
            () -> client.deleteData(result.id())
            .checkEmptyResult("Failed to delete data for %d", result.id())
            .returnEmpty()
            .onStatusCodeError()
        );

        mutating.fork(
            () -> client.reportDeleted(result.id())
                .checkEmptyResult("Failed to report deletion for %d", result.id())
                .returnEmpty()
                .onStatusCodeError()
        );

        mutating.join().throwIfFailed();
      }
    }
  }

  public static class RuntimeExceptionBuilder extends ExceptionBuilder<RuntimeException, RuntimeExceptionBuilder> {

    @Override
    public RuntimeException toException() {
      String message = this.message == null ? null : this.message.toString();
      throw new RuntimeException(message);
    }
  }
}
