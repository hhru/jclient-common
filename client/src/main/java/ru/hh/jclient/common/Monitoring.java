package ru.hh.jclient.common;

public interface Monitoring {

  void countRequest(String upstreamName, String serverDatacenter, String serverAddress, int statusCode, long requestTimeMs, boolean isRequestFinal);

  void countRequestTime(String upstreamName, String serverDatacenter, long requestTimeMs);

  void countRetry(String upstreamName, String serverDatacenter, String serverAddress, int statusCode, int firstStatusCode, int retryCount);
}
