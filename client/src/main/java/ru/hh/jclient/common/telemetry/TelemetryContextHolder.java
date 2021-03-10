//package ru.hh.jclient.common.telemetry;
//
//import io.opentelemetry.api.trace.Span;
//import ru.hh.jclient.common.HttpClientContext;
//
//public class TelemetryContextHolder implements TelemetryContext {
//  private final Span span;
//  private final HttpClientContext httpClientContext;
//
//  public TelemetryContextHolder(Span span, HttpClientContext httpClientContext) {
//    this.span = span;
//    this.httpClientContext = httpClientContext;
//  }
//
//  @Override
//  public Span getSpan() {
//    return span;
//  }
//
//  @Override
//  public HttpClientContext getHttpClientContext() {
//    return httpClientContext;
//  }
//}
