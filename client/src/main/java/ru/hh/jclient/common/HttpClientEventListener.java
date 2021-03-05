package ru.hh.jclient.common;

import ru.hh.jclient.common.telemetry.TelemetryContext;

public interface HttpClientEventListener {
  Request beforeExecute(HttpClient httpClient, Request request, TelemetryContext telemetryContext);
}
