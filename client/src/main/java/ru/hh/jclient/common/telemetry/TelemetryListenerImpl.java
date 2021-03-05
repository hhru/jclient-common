package ru.hh.jclient.common.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.List;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.jclient.common.HttpClientContext;
import ru.hh.jclient.common.Request;

public class TelemetryListenerImpl implements TelemetryListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryListenerImpl.class);

  private static final TextMapGetter<HttpClientContext> GETTER = createGetter();
  private final Tracer tracer;
  private final TextMapPropagator textMapPropagator;

  public TelemetryListenerImpl(Tracer tracer, TextMapPropagator textMapPropagator) {
    this.tracer = tracer;
    this.textMapPropagator = textMapPropagator;
  }

  @Override
  public TelemetryContext beforeExecute(TelemetryContext telemetryContext, Request request) {
    Context context = textMapPropagator.extract(Context.current(), telemetryContext.getHttpClientContext(), GETTER);
    Span span = tracer.spanBuilder(
        request.getUrl()) //todo более общий
        .setParent(context)
        .setSpanKind(SpanKind.CLIENT)
        .setAttribute("requestTimeout", request.getRequestTimeout())
        .setAttribute("readTimeout", request.getReadTimeout())
        .startSpan();
    LOGGER.trace("spanStarted : {}", span);
    return new TelemetryContextHolder(span, telemetryContext.getHttpClientContext());

  }

  @Override
  public TelemetryContext onCompleted(TelemetryContext telemetryContext, Response response) {
    Span span = telemetryContext.getSpan();
    span.setStatus(StatusCode.OK, String.format("code:%d; description:%s", response.getStatusCode(), response.getStatusText()));
    span.end();
    LOGGER.trace("span closed: {}", telemetryContext);
    return telemetryContext;
  }

  @Override
  public TelemetryContext onThrowable(TelemetryContext telemetryContext, Response response) {
    String errorDescription = "";
    if (response != null) {
      errorDescription = String.format("code:%d; description:%s", response.getStatusCode(), response.getStatusText());
    }
    Span span = telemetryContext.getSpan();

    span.setStatus(StatusCode.ERROR, errorDescription);
    span.end();
    LOGGER.trace("span closed: {}", telemetryContext);
    return telemetryContext;
  }

  private static TextMapGetter<HttpClientContext> createGetter() {
    return new TextMapGetter<>() {
      @Override
      public Iterable<String> keys(HttpClientContext carrier) {
        return carrier.getHeaders().keySet();
      }

      @Override
      public String get(HttpClientContext carrier, String key) {
        List<String> header = carrier.getHeaders().get(key);
        if (header == null || header.isEmpty()) {
          return "";
        }
        return header.get(0);
      }
    };
  }
}
