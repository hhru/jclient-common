package ru.hh.jclient.common.telemetry;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import ru.hh.jclient.common.HttpClient;
import ru.hh.jclient.common.HttpClientEventListener;
import ru.hh.jclient.common.HttpHeaders;
import ru.hh.jclient.common.Request;
import ru.hh.jclient.common.RequestBuilder;

public class TelemetryEventListener implements HttpClientEventListener {
  private final TextMapPropagator textMapPropagator;

  public TelemetryEventListener(TextMapPropagator textMapPropagator) {
    this.textMapPropagator = textMapPropagator;
  }

  @Override
  public Request beforeExecute(HttpClient httpClient, Request request, TelemetryContext telemetryContext) {
    if (telemetryContext == null) {
      return request;
    }
    RequestBuilder requestBuilder = new RequestBuilder(request);

    HttpHeaders headers = new HttpHeaders();

    headers.add(request.getHeaders());
    try (Scope ignore = telemetryContext.getSpan().makeCurrent()) {
      textMapPropagator.inject(Context.current(), headers, HttpHeaders::add);
    }
    requestBuilder.setHeaders(headers);
    return requestBuilder.build();
  }
}
