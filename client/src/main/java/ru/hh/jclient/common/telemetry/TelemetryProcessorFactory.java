package ru.hh.jclient.common.telemetry;

import ru.hh.jclient.common.RequestDebug;

public interface TelemetryProcessorFactory {
  RequestDebug createRequestDebug();
}
