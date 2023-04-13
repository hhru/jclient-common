package ru.hh.jclient.common;

public interface Monitoring {

  void countRequest(
      String upstreamName, String serverDatacenter, String serverAddress, int statusCode, long requestTimeMillis, boolean isRequestFinal
  );

  void countRequestTime(String upstreamName, String serverDatacenter, long requestTimeMillis);

  void countRetry(String upstreamName, String serverDatacenter, String serverAddress, int statusCode, int firstStatusCode, int triesUsed);

  void countUpdateIgnore(String upstreamName, String serverDatacenter);
}
