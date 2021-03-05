package ru.hh.jclient.common.telemetry;

import io.opentelemetry.api.trace.Span;
import ru.hh.jclient.common.HttpClientContext;

public interface TelemetryContext {
  Span getSpan();
  HttpClientContext getHttpClientContext();
}
