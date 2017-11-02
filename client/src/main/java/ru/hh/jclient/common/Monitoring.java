package ru.hh.jclient.common;

public interface Monitoring {

  void countRequest(String upstreamName, String serverAddress, int statusCode, boolean isRequestFinal);

  void countRequestTime(String upstreamName, long requestTimeMs);

  void countRetry(String upstreamName, String serverAddress, int statusCode, int firstStatusCode, int retryCount);
}
