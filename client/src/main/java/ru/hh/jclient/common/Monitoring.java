package ru.hh.jclient.common;

public interface Monitoring {

  void countRequest(String upstreamName, String serverAddress, long requestTimeMs, int statusCode, boolean isRequestFinal);

  void countRetry(String upstreamName, String serverAddress, int statusCode, int firstStatusCode, int retryCount);
}
