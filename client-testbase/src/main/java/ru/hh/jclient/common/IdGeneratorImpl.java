package ru.hh.jclient.common;

import io.opentelemetry.sdk.trace.IdGenerator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdGeneratorImpl implements IdGenerator {
  private static final Logger LOGGER = LoggerFactory.getLogger(IdGeneratorImpl.class);
  private final HttpClientContext contextSupplier;
//  private final Storage<HttpClientContext> contextSupplier;

  public IdGeneratorImpl(HttpClientContext contextSupplier) {
//  public IdGeneratorImpl(Storage<HttpClientContext> contextSupplier) {
    this.contextSupplier = contextSupplier;
  }

  @Override
  public String generateSpanId() {
    return IdGenerator.random().generateSpanId();
  }

  @Override
  public String generateTraceId() {
    List<String> requestId = contextSupplier.getHeaders().get(HttpHeaderNames.X_REQUEST_ID);
    if (requestId == null || requestId.isEmpty()) {
      LOGGER.warn("unavailable requestId");
      return IdGenerator.random().generateTraceId();
    } else {
      return requestId.get(0);
    }
  }
}
